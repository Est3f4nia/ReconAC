package com.tup.reconac.modules.vulnEnum.services;

import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.feature.puerto.repositories.PuertoRepository;
import com.tup.reconac.modules.vulnEnum.dtos.*;
import com.tup.reconac.modules.vulnEnum.services.interfaces.IMetricasAuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricasAuditoriaService
        implements IMetricasAuditoriaService {

    private final EscaneoRepository escaneoRepository;
    private final ActivoRepository activoRepository;
    private final PuertoRepository puertoRepository;
    private final EpssService epssService;
    private final KevService kevService;
    private final ObjectMapper objectMapper;
    private final com.tup.reconac.modules.vulnEnum.repositories.CveRepository cveRepository;
    private final com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService auditoriaConsult;

    @Override
    public DashboardAuditoriaResponse getDashboard(UUID auditoriaId) {
        auditoriaConsult.verifyAuditoriaOwnership(auditoriaId);

        List<Escaneo> escaneos =
                escaneoRepository
                        .findByAuditoriaIdOrderByCreadoADesc(auditoriaId);

        if (escaneos.isEmpty()) {
            return emptyDashboard();
        }

        List<UUID> escaneoIds = escaneos.stream()
                .map(Escaneo::getId)
                .toList();

        List<Activo> activos =
                activoRepository.findByEscaneoIdIn(escaneoIds);

        Map<UUID, List<Activo>> activosPorEscaneo =
                activos.stream()
                        .collect(Collectors.groupingBy(
                                Activo::getEscaneoId
                        ));

        List<UUID> activoIds = activos.stream()
                .map(Activo::getId)
                .toList();

        List<Puerto> puertos = activoIds.isEmpty()
                ? List.of()
                : puertoRepository.findByActivoIdIn(activoIds);

        Map<UUID, Long> puertosPorActivo =
                puertos.stream()
                        .collect(Collectors.groupingBy(
                                Puerto::getActivoId,
                                Collectors.counting()
                        ));

        List<ScanMetrics> metrics = escaneos.stream()
                .map(escaneo ->
                        calculateScanMetrics(
                                escaneo,
                                activosPorEscaneo,
                                puertosPorActivo
                        )
                )
                .toList();

        DashboardKpisResponse kpis =
                calculateKpis(
                        escaneos,
                        activos,
                        metrics
                );

        List<EjecucionHistorialResponse> historial =
                metrics.stream()
                        .map(metric ->
                                new EjecucionHistorialResponse(
                                        metric.escaneoId(),
                                        metric.fecha(),
                                        metric.estado(),
                                        metric.progreso(),
                                        metric.objetivos(),
                                        metric.activos(),
                                        metric.puertos(),
                                        metric.cves(),
                                        metric.cvesCriticos(),
                                        metric.cvssPromedio(),
                                        escaneos.stream().filter(e -> e.getId().equals(metric.escaneoId()))
                                                .findFirst().map(Escaneo::getMensajeError).orElse(null)
                                )
                        )
                        .toList();

        List<RiesgoTemporalResponse> riesgoTemporal =
                metrics.stream()
                        .map(metric ->
                                new RiesgoTemporalResponse(
                                        metric.escaneoId(),
                                        metric.fecha(),
                                        calcularNivelRiesgo(
                                                metric.cvssPromedio()
                                        ),
                                        metric.cvssPromedio(),
                                        metric.cves(),
                                        metric.cvesCriticos(),
                                        metric.cvesExplotados()
                                )
                        )
                        .toList();

        CveDesgloseResponse vulnerabilidades =
                buildCveDesglose(metrics);

        HostDesgloseResponse hosts = buildHostDesglose(escaneos, metrics);

        return new DashboardAuditoriaResponse(
                kpis,
                historial,
                riesgoTemporal,
                vulnerabilidades,
                hosts
        );
    }

    private ScanMetrics calculateScanMetrics(
            Escaneo escaneo,
            Map<UUID, List<Activo>> activosPorEscaneo,
            Map<UUID, Long> puertosPorActivo
    ) {

        List<Activo> activos =
                activosPorEscaneo.getOrDefault(
                        escaneo.getId(),
                        List.of()
                );

        long puertos = activos.stream()
                .mapToLong(activo ->
                        puertosPorActivo.getOrDefault(
                                activo.getId(),
                                0L
                        )
                )
                .sum();

        if (escaneo.getResultado() == null ||
                escaneo.getResultado().isBlank()) {

            return ScanMetrics.empty(
                    escaneo,
                    activos.size(),
                    puertos
            );
        }

        try {

            JsonNode root =
                    objectMapper.readTree(
                            escaneo.getResultado()
                    );

            JsonNode vulnerabilities = vulnerabilityNodes(root.path("apiResults"));

            List<CveData> cvesData = new ArrayList<>();

            if (vulnerabilities.isArray()) {

                for (JsonNode vulnerability : vulnerabilities) {

                    String cveId =
                            vulnerability
                                    .path("cve_id")
                                    .asText(null);

                    if (cveId == null || cveId.isBlank()) {
                        continue;
                    }

                    cveId = cveId.trim().toUpperCase(Locale.ROOT);

                    BigDecimal cvss =
                            decimal(
                                    vulnerability,
                                    "cvss_score"
                            );

                    Set<String> cwes =
                            new HashSet<>();

                    JsonNode cweNodes =
                            vulnerability.path("cwes");

                    if (cweNodes.isArray()) {

                        for (JsonNode cwe : cweNodes) {

                            String id =
                                    cwe.path("id")
                                            .asText(null);

                            if (id != null && !id.isBlank()) {
                                cwes.add(id);
                            }
                        }
                    }

                    /*
                     * EPSS y KEV se enriquecen posteriormente
                     * mediante sus respectivos services.
                     */
                    cvesData.add(
                            new CveData(
                                    cveId,
                                    cvss,
                                    null,
                                    false,
                                    cwes.stream()
                                            .sorted()
                                            .toList()
                            )
                    );
                }
            }

            /*
             * Un mismo CVE puede aparecer varias veces
             * por afectar distintos hosts/servicios.
             *
             * Para las métricas generales de la ejecución
             * contamos cada CVE una sola vez.
             */
            cvesData = deduplicateCves(cvesData);

            List<String> cveIds = cvesData.stream()
                    .map(CveData::id)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            // Una lectura del dashboard no dispara llamadas externas ni escrituras.
            Map<String, com.tup.reconac.modules.vulnEnum.models.Cve> catalog = cveIds.isEmpty()
                    ? Map.of() : cveRepository.findAllByCveIn(cveIds).stream()
                    .collect(Collectors.toMap(com.tup.reconac.modules.vulnEnum.models.Cve::getCve, c -> c));
            cvesData = cvesData.stream().map(cve -> {
                var saved = catalog.get(cve.id());
                return new CveData(cve.id(), cve.cvss(), saved == null ? null : saved.getEpss(),
                        saved != null && Boolean.TRUE.equals(saved.getKev()), cve.cwes());
            }).toList();
            long cves = cvesData.size();

            long criticos = 0;
            long altos = 0;
            long medios = 0;
            long bajos = 0;
            long explotados = 0;

            BigDecimal sumaCvss =
                    BigDecimal.ZERO;

            long cvssCount = 0;

            for (CveData cve : cvesData) {

                BigDecimal cvss = cve.cvss();

                if (cvss != null) {

                    sumaCvss =
                            sumaCvss.add(cvss);

                    cvssCount++;

                    if (cvss.compareTo(
                            BigDecimal.valueOf(9)
                    ) >= 0) {

                        criticos++;

                    } else if (cvss.compareTo(
                            BigDecimal.valueOf(7)
                    ) >= 0) {

                        altos++;

                    } else if (cvss.compareTo(
                            BigDecimal.valueOf(4)
                    ) >= 0) {

                        medios++;

                    } else {

                        bajos++;
                    }
                }

                if (cve.explotacionActiva()) {
                    explotados++;
                }
            }

            BigDecimal promedio =
                    cvssCount == 0
                            ? null
                            : sumaCvss.divide(
                            BigDecimal.valueOf(cvssCount),
                            2,
                            RoundingMode.HALF_UP
                    );

            return new ScanMetrics(
                    escaneo.getId(),
                    escaneo.getCreadoA(),
                    escaneo.getEstado().name(),
                    escaneo.getProgreso(),
                    escaneo.getObjetivos() == null
                            ? List.of()
                            : List.of(
                            escaneo.getObjetivos()
                    ),
                    activos.size(),
                    puertos,
                    cves,
                    criticos,
                    altos,
                    medios,
                    bajos,
                    promedio,
                    explotados,
                    cvesData
            );

        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron leer las métricas del escaneo " + escaneo.getId(), e);
        }
    }

    private JsonNode vulnerabilityNodes(JsonNode apiResults) {
        if (apiResults.path("vulnerabilities").isArray()) return apiResults.path("vulnerabilities");
        var result = objectMapper.createArrayNode();
        for (JsonNode entry : apiResults) {
            if (entry.path("vulnerabilities").isArray()) {
                for (JsonNode vulnerability : entry.path("vulnerabilities")) result.add(vulnerability);
            }
        }
        return result;
    }

    private HostDesgloseResponse buildHostDesglose(List<Escaneo> scans, List<ScanMetrics> metrics) {
        Map<String, Map<String, CveData>> byHost = new LinkedHashMap<>();
        Map<String, String> names = new HashMap<>();
        for (Escaneo scan : scans) {
            if (scan.getResultado() == null || scan.getResultado().isBlank()) continue;
            JsonNode root = objectMapper.readTree(scan.getResultado());
            JsonNode byCpe = root.path("apiResults");
            // Los registros antiguos planos no permiten atribuir CVEs a un host.
            if (byCpe.path("vulnerabilities").isArray()) continue;
            var scanMetrics = metrics.stream().filter(m -> m.escaneoId().equals(scan.getId())).findFirst().orElseThrow();
            var byId = scanMetrics.cvesData().stream().collect(Collectors.toMap(CveData::id, c -> c));
            for (JsonNode host : root.path("hosts")) {
                String ip = host.path("ip").asText(null);
                if (ip == null) continue;
                names.putIfAbsent(ip, host.path("hostname").asText(null));
                var found = byHost.computeIfAbsent(ip, ignored -> new HashMap<>());
                for (JsonNode port : host.path("puertos")) {
                    for (JsonNode cpe : port.path("cpes")) {
                        for (JsonNode vulnerability : byCpe.path(cpe.asText()).path("vulnerabilities")) {
                            String id = vulnerability.path("cve_id").asText("").toUpperCase(Locale.ROOT);
                            CveData value = byId.get(id);
                            if (value != null) found.merge(id, value, this::mergeCveData);
                        }
                    }
                }
            }
        }
        var hosts = byHost.entrySet().stream().map(entry -> {
            var values = entry.getValue().values();
            return new HostVulnerabilidadResponse(entry.getKey(), names.get(entry.getKey()), values.size(),
                    values.stream().filter(v -> v.cvss() != null && v.cvss().compareTo(BigDecimal.valueOf(9)) >= 0).count(),
                    values.stream().map(CveData::cvss).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(null),
                    values.stream().map(CveData::epss).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(null));
        }).toList();
        return new HostDesgloseResponse(
                hosts.stream().sorted(Comparator.comparingLong(HostVulnerabilidadResponse::vulnerabilidadesCriticas)
                        .reversed().thenComparing(HostVulnerabilidadResponse::ip)).toList(),
                hosts.stream().filter(h -> h.epssMaximo() != null)
                        .sorted(Comparator.comparing(HostVulnerabilidadResponse::epssMaximo).reversed()
                                .thenComparing(HostVulnerabilidadResponse::ip)).toList());
    }
    private DashboardKpisResponse calculateKpis(
            List<Escaneo> escaneos,
            List<Activo> activos,
            List<ScanMetrics> metrics
    ) {

        /*
         * Deduplicación global:
         *
         * Un CVE puede aparecer en varias ejecuciones,
         * pero para los KPIs generales de la auditoría
         * se cuenta una sola vez.
         */
        List<CveData> uniqueCveData =
                metrics.stream()
                        .flatMap(metric ->
                                metric.cvesData().stream()
                        )
                        .filter(cve -> cve.id() != null)
                        .collect(Collectors.toMap(
                                cve -> cve.id()
                                        .toUpperCase(Locale.ROOT),
                                cve -> cve,
                                this::mergeCveData
                        ))
                        .values()
                        .stream()
                        .toList();

        long cves = uniqueCveData.size();

        long criticos = uniqueCveData.stream()
                .filter(cve ->
                        cve.cvss() != null &&
                                cve.cvss().compareTo(
                                        BigDecimal.valueOf(9)
                                ) >= 0
                )
                .count();

        long altos = uniqueCveData.stream()
                .filter(cve ->
                        cve.cvss() != null &&
                                cve.cvss().compareTo(
                                        BigDecimal.valueOf(7)
                                ) >= 0 &&
                                cve.cvss().compareTo(
                                        BigDecimal.valueOf(9)
                                ) < 0
                )
                .count();

        long medios = uniqueCveData.stream()
                .filter(cve ->
                        cve.cvss() != null &&
                                cve.cvss().compareTo(
                                        BigDecimal.valueOf(4)
                                ) >= 0 &&
                                cve.cvss().compareTo(
                                        BigDecimal.valueOf(7)
                                ) < 0
                )
                .count();

        long bajos = uniqueCveData.stream()
                .filter(cve ->
                        cve.cvss() != null &&
                                cve.cvss().compareTo(
                                        BigDecimal.valueOf(4)
                                ) < 0
                )
                .count();

        long explotados = uniqueCveData.stream()
                .filter(CveData::explotacionActiva)
                .count();

        List<BigDecimal> cvss =
                uniqueCveData.stream()
                        .map(CveData::cvss)
                        .filter(Objects::nonNull)
                        .toList();

        BigDecimal cvssPromedio = null;

        if (!cvss.isEmpty()) {

            BigDecimal suma =
                    cvss.stream()
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            cvssPromedio =
                    suma.divide(
                            BigDecimal.valueOf(cvss.size()),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        List<BigDecimal> epss =
                uniqueCveData.stream()
                        .map(CveData::epss)
                        .filter(Objects::nonNull)
                        .toList();

        BigDecimal epssPromedio = null;

        if (!epss.isEmpty()) {

            BigDecimal suma =
                    epss.stream()
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            epssPromedio =
                    suma.divide(
                            BigDecimal.valueOf(epss.size()),
                            4,
                            RoundingMode.HALF_UP
                    );
        }

        long puertos = metrics.stream()
                .mapToLong(ScanMetrics::puertos)
                .sum();

        long activosUnicos = activos.stream()
                .map(Activo::getHost)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(host -> !host.isBlank())
                .distinct()
                .count();

        return new DashboardKpisResponse(
                escaneos.size(),
                activosUnicos,
                puertos,
                cves,
                criticos,
                altos,
                medios,
                bajos,
                cvssPromedio,
                explotados,
                epssPromedio
        );
    }

    private CveDesgloseResponse buildCveDesglose(
            List<ScanMetrics> metrics
    ) {

        Map<String, CveAccumulator> accumulator =
                new HashMap<>();

        for (ScanMetrics metric : metrics) {

            for (CveData cve : metric.cvesData()) {

                if (cve.id() == null) {
                    continue;
                }

                CveAccumulator current =
                        accumulator.computeIfAbsent(
                                cve.id(),
                                CveAccumulator::new
                        );

                current.frecuencia++;

                /*
                 * Conservamos el CVSS máximo observado.
                 */
                if (cve.cvss() != null &&
                        (current.cvss == null ||
                                cve.cvss().compareTo(
                                        current.cvss
                                ) > 0)) {

                    current.cvss = cve.cvss();
                }

                /*
                 * Conservamos el EPSS máximo observado.
                 */
                if (cve.epss() != null &&
                        (current.epss == null ||
                                cve.epss().compareTo(
                                        current.epss
                                ) > 0)) {

                    current.epss = cve.epss();
                }

                /*
                 * Si alguna ejecución indica que el CVE
                 * está en KEV, queda marcado como explotado.
                 */
                current.explotacionActiva |=
                        cve.explotacionActiva();

                current.cwes.addAll(
                        cve.cwes()
                );
            }
        }

        List<CveResumenResponse> all =
                accumulator.values()
                        .stream()
                        .map(CveAccumulator::toResponse)
                        .toList();

        List<CveResumenResponse> masComunes =
                all.stream()
                        .sorted(
                                Comparator.comparingLong(
                                        CveResumenResponse::frecuencia
                                ).reversed()
                        )
                        .limit(10)
                        .toList();

        List<CveResumenResponse> explotacionActiva =
                all.stream()
                        .filter(
                                CveResumenResponse
                                        ::explotacionActiva
                        )
                        .sorted(
                                Comparator.comparing(
                                        CveResumenResponse
                                                ::cvssScore,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                        )
                        .limit(10)
                        .toList();

        List<CveResumenResponse> mayorCriticidad =
                all.stream()
                        .sorted(
                                Comparator.comparing(
                                        CveResumenResponse
                                                ::cvssScore,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                        )
                        .limit(10)
                        .toList();

        List<CveResumenResponse>
                mayorProbabilidadExplotacion =
                all.stream()
                        .filter(cve ->
                                cve.epssScore() != null
                        )
                        .sorted(
                                Comparator.comparing(
                                        CveResumenResponse
                                                ::epssScore,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                        )
                        .limit(10)
                        .toList();

        return new CveDesgloseResponse(
                masComunes,
                explotacionActiva,
                mayorCriticidad,
                mayorProbabilidadExplotacion
        );
    }

    private CveData mergeCveData(
            CveData current,
            CveData incoming
    ) {

        BigDecimal cvss;

        if (current.cvss() == null) {
            cvss = incoming.cvss();

        } else if (incoming.cvss() == null) {
            cvss = current.cvss();

        } else {
            cvss = current.cvss().max(
                    incoming.cvss()
            );
        }

        BigDecimal epss;

        if (current.epss() == null) {
            epss = incoming.epss();

        } else if (incoming.epss() == null) {
            epss = current.epss();

        } else {
            epss = current.epss().max(
                    incoming.epss()
            );
        }

        Set<String> cwes =
                new HashSet<>();

        if (current.cwes() != null) {
            cwes.addAll(current.cwes());
        }

        if (incoming.cwes() != null) {
            cwes.addAll(incoming.cwes());
        }

        return new CveData(
                current.id(),
                cvss,
                epss,
                current.explotacionActiva()
                        || incoming.explotacionActiva(),
                cwes.stream()
                        .sorted()
                        .toList()
        );
    }

    private List<CveData> deduplicateCves(
            List<CveData> cves
    ) {

        Map<String, CveData> unique =
                new HashMap<>();

        for (CveData cve : cves) {

            if (cve.id() == null) {
                continue;
            }

            String cveId =
                    cve.id()
                            .trim()
                            .toUpperCase(Locale.ROOT);

            CveData normalized =
                    new CveData(
                            cveId,
                            cve.cvss(),
                            cve.epss(),
                            cve.explotacionActiva(),
                            cve.cwes() == null
                                    ? List.of()
                                    : cve.cwes()
                    );

            unique.merge(
                    cveId,
                    normalized,
                    this::mergeCveData
            );
        }

        return unique.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                CveData::id
                        )
                )
                .toList();
    }

    private String calcularNivelRiesgo(
            BigDecimal cvss
    ) {

        if (cvss == null) {
            return "DESCONOCIDO";
        }

        if (cvss.compareTo(
                BigDecimal.valueOf(9)
        ) >= 0) {
            return "CRITICO";
        }

        if (cvss.compareTo(
                BigDecimal.valueOf(7)
        ) >= 0) {
            return "ALTO";
        }

        if (cvss.compareTo(
                BigDecimal.valueOf(4)
        ) >= 0) {
            return "MEDIO";
        }

        return "BAJO";
    }

    private BigDecimal decimal(
            JsonNode node,
            String field
    ) {

        JsonNode value = node.get(field);

        if (value == null ||
                !value.isNumber()) {

            return null;
        }

        return value.decimalValue()
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private DashboardAuditoriaResponse emptyDashboard() {

        return new DashboardAuditoriaResponse(

                new DashboardKpisResponse(
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        null,
                        0,
                        null
                ),

                List.of(),
                List.of(),

                new CveDesgloseResponse(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ),

                new HostDesgloseResponse(
                        List.of(),
                        List.of()
                )
        );
    }

    private record ScanMetrics(
            UUID escaneoId,
            LocalDateTime fecha,
            String estado,
            Integer progreso,
            List<String> objetivos,
            long activos,
            long puertos,
            long cves,
            long cvesCriticos,
            long cvesAltos,
            long cvesMedios,
            long cvesBajos,
            BigDecimal cvssPromedio,
            long cvesExplotados,
            List<CveData> cvesData
    ) {

        static ScanMetrics empty(
                Escaneo escaneo,
                long activos,
                long puertos
        ) {

            return new ScanMetrics(
                    escaneo.getId(),
                    escaneo.getCreadoA(),
                    escaneo.getEstado().name(),
                    escaneo.getProgreso(),
                    escaneo.getObjetivos() == null
                            ? List.of()
                            : List.of(
                            escaneo.getObjetivos()
                    ),
                    activos,
                    puertos,
                    0,
                    0,
                    0,
                    0,
                    0,
                    null,
                    0,
                    List.of()
            );
        }
    }

    private record CveData(
            String id,
            BigDecimal cvss,
            BigDecimal epss,
            boolean explotacionActiva,
            List<String> cwes
    ) {
    }

    private static class CveAccumulator {

        private final String id;
        private long frecuencia;
        private BigDecimal cvss;
        private boolean explotacionActiva;
        private BigDecimal epss;

        private final Set<String> cwes =
                new HashSet<>();

        private CveAccumulator(String id) {
            this.id = id;
        }

        private CveResumenResponse toResponse() {

            return new CveResumenResponse(
                    id,
                    frecuencia,
                    cvss,
                    epss,
                    explotacionActiva,
                    cwes.stream()
                            .sorted()
                            .toList()
            );
        }
    }
}
