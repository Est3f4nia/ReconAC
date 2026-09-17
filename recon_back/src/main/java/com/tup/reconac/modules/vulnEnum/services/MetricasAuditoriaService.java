package com.tup.reconac.modules.vulnEnum.services;

import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.feature.puerto.repositories.PuertoRepository;
import com.tup.reconac.modules.vulnEnum.dtos.*;
import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.models.CpeCve;
import com.tup.reconac.modules.vulnEnum.models.PuertoCpe;
import com.tup.reconac.modules.vulnEnum.repositories.CpeCveRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CpeRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CveRepository;
import com.tup.reconac.modules.vulnEnum.repositories.PuertoCpeRepository;
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
public class MetricasAuditoriaService implements IMetricasAuditoriaService {

    private final EscaneoRepository escaneoRepository;
    private final ActivoRepository activoRepository;
    private final PuertoRepository puertoRepository;
    private final ObjectMapper objectMapper;
    private final CveRepository cveRepository;
    private final AuditoriaConsultService auditoriaConsult;
    private final PuertoCpeRepository puertoCpeRepository;
    private final CpeRepository cpeRepository;
    private final CpeCveRepository cpeCveRepository;

    @Override
    public DashboardAuditoriaResponse getDashboard(UUID auditoriaId) {

        auditoriaConsult.verifyAuditoriaOwnership(auditoriaId);

        List<Escaneo> escaneos = escaneoRepository.findByAuditoriaIdOrderByCreadoADesc(auditoriaId);
        if (escaneos.isEmpty()) return emptyDashboard();


        Map<UUID, Escaneo> escaneosPorId =  escaneos
                .stream()
                .collect(Collectors.toMap(Escaneo::getId, escaneo -> escaneo));

        List<UUID> escaneoIds = escaneos
                .stream()
                .map(Escaneo::getId)
                .toList();

        /*
         * ========================================================
         * Activos
         * ========================================================
         */

        List<Activo> activos = activoRepository.findByEscaneoIdIn(escaneoIds);

        Map<UUID, List<Activo>> activosPorEscaneo = activos
                .stream()
                .collect(Collectors.groupingBy(Activo::getEscaneoId));

        List<UUID> activoIds =
                activos.stream()
                        .map(Activo::getId)
                        .toList();

        /*
         * ========================================================
         * Puertos
         * ========================================================
         */

        List<Puerto> puertos = activoIds.isEmpty()
                ? List.of() : puertoRepository.findByActivoIdIn(activoIds);

        Map<UUID, Long> puertosPorActivo = puertos
                .stream()
                .collect(Collectors.groupingBy(Puerto::getActivoId, Collectors.counting()));

        /*
         * ========================================================
         * Métricas por ejecución
         * ========================================================
         */

        List<ScanMetrics> metrics = escaneos
                .stream()
                .map(escaneo ->
                                calculateScanMetrics(
                                        escaneo,
                                        activosPorEscaneo,
                                        puertosPorActivo
                                )
                )
                .toList();

        /*
         * ========================================================
         * KPIs globales
         * ========================================================
         */

        DashboardKpisResponse kpis = calculateKpis(
                escaneos,
                activos,
                metrics
        );

        /*
         * ========================================================
         * Historial
         * ========================================================
         */

        List<EjecucionHistorialResponse> historial = metrics
                .stream()
                .map(metric -> {

                            Escaneo escaneo =
                                    escaneosPorId.get(
                                            metric.escaneoId()
                                    );

                            return new EjecucionHistorialResponse(
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

                                    escaneo != null
                                            ? escaneo.getMensajeError()
                                            : null
                            );
                        })
                        .toList();

        /*
         * ========================================================
         * Evolución temporal del riesgo
         * ========================================================
         */

        List<RiesgoTemporalResponse> riesgoTemporal =
                metrics.stream()
                        .map(
                                metric ->
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

        /*
         * ========================================================
         * Desglose de vulnerabilidades
         * ========================================================
         */

        CveDesgloseResponse vulnerabilidades = buildCveDesglose(
                metrics,
                activos,
                puertos
        );

        /*
         * ========================================================
         * Desglose de hosts
         * ========================================================
         */

        HostDesgloseResponse hosts = buildHostDesglose(
                activos,
                puertos
        );

        /*
         * ========================================================
         * Respuesta final
         * ========================================================
         */

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
                                    .asString(null);

                    if (cveId == null || cveId.isBlank()) {
                        continue;
                    }

                    cveId = cveId.trim().toUpperCase(Locale.ROOT);

                    BigDecimal cvss =
                            decimal(
                                    vulnerability
                            );

                    Set<String> cwes =
                            new HashSet<>();

                    JsonNode cweNodes =
                            vulnerability.path("cwes");

                    if (cweNodes.isArray()) {

                        for (JsonNode cwe : cweNodes) {

                            String id = cwe.path("id").asString(null);

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
                    .map(this::normalizeCveId)
                    .distinct()
                    .toList();

            Map<String, com.tup.reconac.modules.vulnEnum.models.Cve> catalog =
                    cveIds.isEmpty()
                            ? Map.of()
                            : cveRepository.findAllByCveIn(cveIds)
                            .stream()
                            .filter(cve -> cve.getCve() != null)
                            .collect(
                                    Collectors.toMap(
                                            cve -> normalizeCveId(cve.getCve()),
                                            cve -> cve,
                                            (actual, duplicado) -> actual
                                    )
                            );

            cvesData = cvesData.stream()
                    .map(cve -> {
                        var saved = catalog.get(normalizeCveId(cve.id()));
                        return new CveData(
                                cve.id(),
                                cve.cvss(),
                                saved != null
                                        ? saved.getEpss()
                                        : null,
                                saved != null &&
                                        Boolean.TRUE.equals(saved.getKev()),
                                cve.cwes()
                        );
                    })
                    .toList();

            long cves = cvesData.size();
            long criticos = 0;
            long altos = 0;
            long medios = 0;
            long bajos = 0;
            long explotados = 0;
            BigDecimal sumaCvss = BigDecimal.ZERO;
            long cvssCount = 0;

            for (CveData cve : cvesData) {
                BigDecimal cvss = cve.cvss();
                if (cvss != null) {
                    sumaCvss = sumaCvss.add(cvss);
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

    private String normalizeCveId(String id) {
        return id == null
                ? null
                : id.trim().toUpperCase(Locale.ROOT);
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

    private HostDesgloseResponse buildHostDesglose(
            List<Activo> activos,
            List<Puerto> puertos
    ) {

        if (activos.isEmpty() || puertos.isEmpty()) {
            return new HostDesgloseResponse(
                    List.of(),
                    List.of()
            );
        }

        /*
         * ========================================================
         * 1. Activos por ID
         * ========================================================
         */

        Map<UUID, Activo> activosPorId =
                activos.stream()
                        .collect(
                                Collectors.toMap(
                                        Activo::getId,
                                        activo -> activo
                                )
                        );

        /*
         * ========================================================
         * 2. Puertos por ID
         * ========================================================
         */

        Map<UUID, Puerto> puertosPorId =
                puertos.stream()
                        .collect(
                                Collectors.toMap(
                                        Puerto::getId,
                                        puerto -> puerto
                                )
                        );

        List<UUID> puertoIds =
                puertos.stream()
                        .map(Puerto::getId)
                        .toList();

        /*
         * ========================================================
         * 3. Puerto -> CPE
         * ========================================================
         */

        List<PuertoCpe> puertoCpes =
                puertoIds.isEmpty()
                        ? List.of()
                        : puertoCpeRepository
                        .findByPuertoIdIn(
                                puertoIds
                        );

        if (puertoCpes.isEmpty()) {
            return new HostDesgloseResponse(
                    List.of(),
                    List.of()
            );
        }

        Set<UUID> cpeIds =
                puertoCpes.stream()
                        .map(PuertoCpe::getCpeId)
                        .collect(
                                Collectors.toSet()
                        );

        /*
         * ========================================================
         * 4. CPEs
         * ========================================================
         */

        Map<UUID, Cpe> cpesPorId =
                cpeRepository
                        .findAllById(cpeIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Cpe::getId,
                                        cpe -> cpe
                                )
                        );

        /*
         * ========================================================
         * 5. CPE -> CVE
         * ========================================================
         */

        List<CpeCve> cpeCves =
                cpeIds.isEmpty()
                        ? List.of()
                        : cpeCveRepository
                        .findByCpeIdIn(
                                cpeIds
                        );

        if (cpeCves.isEmpty()) {
            return new HostDesgloseResponse(
                    List.of(),
                    List.of()
            );
        }

        Set<UUID> cveIds =
                cpeCves.stream()
                        .map(CpeCve::getCveId)
                        .collect(
                                Collectors.toSet()
                        );

        /*
         * ========================================================
         * 6. Catálogo CVE
         * ========================================================
         */

        Map<UUID, com.tup.reconac.modules.vulnEnum.models.Cve>
                cvesPorId =
                cveRepository
                        .findAllById(cveIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        com.tup.reconac.modules.vulnEnum.models.Cve
                                                ::getId,
                                        cve -> cve
                                )
                        );

        /*
         * ========================================================
         * 7. CPE -> CVEs
         * ========================================================
         */

        Map<UUID, List<CpeCve>> cvesPorCpe =
                cpeCves.stream()
                        .collect(
                                Collectors.groupingBy(
                                        CpeCve::getCpeId
                                )
                        );

        /*
         * ========================================================
         * 8. Puerto -> CPEs
         * ========================================================
         */

        Map<UUID, List<PuertoCpe>> cpesPorPuerto =
                puertoCpes.stream()
                        .collect(
                                Collectors.groupingBy(
                                        PuertoCpe::getPuertoId
                                )
                        );

        /*
         * ========================================================
         * 9. Acumulador por host
         * ========================================================
         */

        Map<String, HostAccumulator> hosts =
                new LinkedHashMap<>();

        for (Puerto puerto : puertos) {

            Activo activo =
                    activosPorId.get(
                            puerto.getActivoId()
                    );

            if (activo == null ||
                    activo.getHost() == null ||
                    activo.getHost().isBlank()) {

                continue;
            }

            String hostIp =
                    activo.getHost().trim();

            HostAccumulator host =
                    hosts.computeIfAbsent(
                            hostIp,
                            ignored ->
                                    new HostAccumulator(
                                            hostIp,
                                            activo.getHostname()
                                    )
                    );

            /*
             * Si el mismo host aparece en varios
             * escaneos y uno tiene hostname,
             * conservamos el dato disponible.
             */
            if ((host.hostname == null ||
                    host.hostname.isBlank()) &&
                    activo.getHostname() != null &&
                    !activo.getHostname().isBlank()) {

                host.hostname =
                        activo.getHostname();
            }

            List<PuertoCpe> relaciones =
                    cpesPorPuerto.getOrDefault(
                            puerto.getId(),
                            List.of()
                    );

            for (PuertoCpe puertoCpe : relaciones) {

                UUID cpeId =
                        puertoCpe.getCpeId();

                Cpe cpe =
                        cpesPorId.get(cpeId);

                if (cpe == null) {
                    continue;
                }

                List<CpeCve> relacionesCve =
                        cvesPorCpe.getOrDefault(
                                cpeId,
                                List.of()
                        );

                for (CpeCve cpeCve : relacionesCve) {

                    var cve =
                            cvesPorId.get(
                                    cpeCve.getCveId()
                            );

                    if (cve == null ||
                            cve.getCve() == null) {

                        continue;
                    }

                    host.addCve(
                            cve,
                            cpe.getUri()
                    );
                }
            }
        }

        /*
         * ========================================================
         * 10. Convertimos acumuladores en DTOs
         * ========================================================
         */

        List<HostVulnerabilidadResponse> all =
                hosts.values()
                        .stream()
                        /*
                         * Un host sin CVEs no debe formar
                         * parte de los rankings de riesgo.
                         */
                        .filter(
                                host ->
                                        !host.cves.isEmpty()
                        )
                        .map(
                                HostAccumulator::toResponse
                        )
                        .toList();

        /*
         * ========================================================
         * 11. Más vulnerabilidades críticas
         * ========================================================
         */

        List<HostVulnerabilidadResponse>
                masVulnerabilidadesCriticas =
                all.stream()
                        /*
                         * Corrige el bug:
                         * los hosts con 0 críticas
                         * ya no aparecen.
                         */
                        .filter(
                                host ->
                                        host.vulnerabilidadesCriticas()
                                                > 0
                        )
                        .sorted(
                                Comparator
                                        .comparingLong(
                                                HostVulnerabilidadResponse
                                                        ::vulnerabilidadesCriticas
                                        )
                                        .reversed()
                                        .thenComparing(
                                                HostVulnerabilidadResponse
                                                        ::cvssMaximo,
                                                Comparator.nullsLast(
                                                        Comparator.reverseOrder()
                                                )
                                        )
                                        .thenComparing(
                                                HostVulnerabilidadResponse
                                                        ::ip
                                        )
                        )
                        .limit(10)
                        .toList();

        /*
         * ========================================================
         * 12. Mayor riesgo de explotación
         *
         * Prioridad:
         *
         * KEV > CVSS > EPSS
         * ========================================================
         */

        List<HostVulnerabilidadResponse>
                mayorRiesgoExplotacion =
                all.stream()
                        .sorted(
                                Comparator
                                        .comparing(
                                                HostVulnerabilidadResponse
                                                        ::kev
                                        )
                                        .reversed()
                                        .thenComparing(
                                                HostVulnerabilidadResponse
                                                        ::cvssMaximo,
                                                Comparator.nullsLast(
                                                        Comparator.reverseOrder()
                                                )
                                        )
                                        .thenComparing(
                                                HostVulnerabilidadResponse
                                                        ::epssMaximo,
                                                Comparator.nullsLast(
                                                        Comparator.reverseOrder()
                                                )
                                        )
                                        .thenComparing(
                                                HostVulnerabilidadResponse
                                                        ::ip
                                        )
                        )
                        .limit(10)
                        .toList();

        return new HostDesgloseResponse(
                masVulnerabilidadesCriticas,
                mayorRiesgoExplotacion
        );
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
            List<ScanMetrics> metrics,
            List<Activo> activos,
            List<Puerto> puertos
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

        /*
         * ========================================================
         * Enriquecimiento desde entidades persistidas
         *
         * Activo -> Puerto -> PuertoCpe -> CpeCve -> Cve
         *
         * Escaneo.resultado puede no contener todas las CVE que
         * posteriormente fueron asociadas mediante el catálogo.
         * ========================================================
         */

        Map<UUID, UUID> escaneoPorActivo =
                activos.stream()
                        .collect(
                                Collectors.toMap(
                                        Activo::getId,
                                        Activo::getEscaneoId
                                )
                        );

        Map<UUID, UUID> activoPorPuerto =
                puertos.stream()
                        .collect(
                                Collectors.toMap(
                                        Puerto::getId,
                                        Puerto::getActivoId
                                )
                        );

        List<UUID> puertoIds =
                puertos.stream()
                        .map(Puerto::getId)
                        .toList();

        List<PuertoCpe> puertoCpes =
                puertoIds.isEmpty()
                        ? List.of()
                        : puertoCpeRepository.findByPuertoIdIn(
                        puertoIds
                );

        /*
         * CPE -> escaneos donde fue observado.
         */
        Map<UUID, Set<UUID>> escaneosPorCpe =
                new HashMap<>();

        for (PuertoCpe relacion : puertoCpes) {

            UUID activoId =
                    activoPorPuerto.get(
                            relacion.getPuertoId()
                    );

            if (activoId == null) {
                continue;
            }

            UUID escaneoId =
                    escaneoPorActivo.get(
                            activoId
                    );

            if (escaneoId == null) {
                continue;
            }

            escaneosPorCpe
                    .computeIfAbsent(
                            relacion.getCpeId(),
                            ignored -> new HashSet<>()
                    )
                    .add(escaneoId);
        }

        Set<UUID> cpeIds =
                puertoCpes.stream()
                        .map(PuertoCpe::getCpeId)
                        .collect(Collectors.toSet());

        List<CpeCve> cpeCves =
                cpeIds.isEmpty()
                        ? List.of()
                        : cpeCveRepository.findByCpeIdIn(
                        cpeIds
                );

        Set<UUID> cveEntityIds =
                cpeCves.stream()
                        .map(CpeCve::getCveId)
                        .collect(Collectors.toSet());

        Map<UUID, com.tup.reconac.modules.vulnEnum.models.Cve> cvesPorId =
                cveRepository.findAllById(cveEntityIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        com.tup.reconac.modules.vulnEnum.models.Cve::getId,
                                        cve -> cve
                                )
                        );

        /*
         * CVE -> escaneos distintos donde aparece.
         *
         * Evita contar varias veces una CVE por estar asociada
         * a varios puertos/CPE dentro del mismo escaneo.
         */
        Map<String, Set<UUID>> escaneosPorCve =
                new HashMap<>();

        for (CpeCve relacion : cpeCves) {

            var saved =
                    cvesPorId.get(
                            relacion.getCveId()
                    );

            if (saved == null ||
                    saved.getCve() == null ||
                    saved.getCve().isBlank()) {

                continue;
            }

            String cveId =
                    saved.getCve()
                            .trim()
                            .toUpperCase(Locale.ROOT);

            CveAccumulator current =
                    accumulator.computeIfAbsent(
                            cveId,
                            CveAccumulator::new
                    );

            /*
             * El catálogo persistido es la fuente de verdad
             * para CVSS, EPSS y KEV.
             */
            if (saved.getCvss() != null &&
                    (current.cvss == null ||
                            saved.getCvss()
                                    .compareTo(current.cvss) > 0)) {

                current.cvss =
                        saved.getCvss();
            }

            if (saved.getEpss() != null &&
                    (current.epss == null ||
                            saved.getEpss()
                                    .compareTo(current.epss) > 0)) {

                current.epss =
                        saved.getEpss();
            }

            current.explotacionActiva |=
                    Boolean.TRUE.equals(
                            saved.getKev()
                    );

            Set<UUID> scanIds =
                    escaneosPorCpe.getOrDefault(
                            relacion.getCpeId(),
                            Set.of()
                    );

            escaneosPorCve
                    .computeIfAbsent(
                            cveId,
                            ignored -> new HashSet<>()
                    )
                    .addAll(scanIds);
        }

        /*
         * La frecuencia representa cantidad de ejecuciones
         * distintas en las que apareció la CVE.
         */
        for (Map.Entry<String, Set<UUID>> entry :
                escaneosPorCve.entrySet()) {

            CveAccumulator current =
                    accumulator.get(
                            entry.getKey()
                    );

            if (current != null) {
                current.frecuencia =
                        Math.max(
                                current.frecuencia,
                                entry.getValue().size()
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

    private BigDecimal decimal(JsonNode node) {

        JsonNode value = node.get("cvss_score");

        if (value == null || !value.isNumber()) return null;

        return value.decimalValue().setScale(2, RoundingMode.HALF_UP);
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

    /* ============================================================
     * Host accumulator
     * ============================================================ */

    private static class HostAccumulator {

        private final String ip;

        private String hostname;

        /*
         * CVE ID -> CVE detectada en el host.
         */
        private final Map<String, HostCveAccumulator>
                cves =
                new HashMap<>();

        private HostAccumulator(
                String ip,
                String hostname
        ) {
            this.ip = ip;
            this.hostname = hostname;
        }

        private void addCve(
                com.tup.reconac.modules.vulnEnum.models.Cve cve,
                String cpe
        ) {

            String cveId =
                    cve.getCve()
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            );

            HostCveAccumulator current = cves.computeIfAbsent(
                            cveId,
                            ignored ->
                                    new HostCveAccumulator(
                                            cveId
                                    )
                    );

            current.merge(
                    cve,
                    cpe
            );
        }

        private HostVulnerabilidadResponse toResponse() {

            List<HostCveResponse> all =
                    cves.values()
                            .stream()
                            .map(
                                    HostCveAccumulator
                                            ::toResponse
                            )
                            .toList();

            long criticas =
                    all.stream()
                            .filter(
                                    cve ->
                                            cve.cvssScore() != null &&
                                                    cve.cvssScore()
                                                            .compareTo(
                                                                    BigDecimal.valueOf(
                                                                            9
                                                                    )
                                                            ) >= 0
                            )
                            .count();

            BigDecimal cvssMaximo =
                    all.stream()
                            .map(
                                    HostCveResponse
                                            ::cvssScore
                            )
                            .filter(
                                    Objects::nonNull
                            )
                            .max(
                                    BigDecimal::compareTo
                            )
                            .orElse(null);

            BigDecimal epssMaximo =
                    all.stream()
                            .map(
                                    HostCveResponse
                                            ::epssScore
                            )
                            .filter(
                                    Objects::nonNull
                            )
                            .max(
                                    BigDecimal::compareTo
                            )
                            .orElse(null);

            boolean kev =
                    all.stream()
                            .anyMatch(
                                    HostCveResponse::kev
                            );

            /*
             * CVEs críticas del host.
             */
            List<HostCveResponse> cvesCriticas =
                    all.stream()
                            .filter(
                                    cve ->
                                            cve.cvssScore() != null &&
                                                    cve.cvssScore()
                                                            .compareTo(
                                                                    BigDecimal.valueOf(
                                                                            9
                                                                    )
                                                            ) >= 0
                            )
                            .sorted(
                                    Comparator
                                            .comparing(
                                                    HostCveResponse
                                                            ::cvssScore,
                                                    Comparator.nullsLast(
                                                            Comparator.reverseOrder()
                                                    )
                                            )
                                            .thenComparing(
                                                    HostCveResponse
                                                            ::cveId
                                            )
                            )
                            .toList();

            /*
             * Priorización:
             *
             * KEV > CVSS > EPSS.
             */
            List<HostCveResponse> cvesPrioritarias =
                    all.stream()
                            .sorted(
                                    Comparator
                                            .comparing(
                                                    HostCveResponse
                                                            ::kev
                                            )
                                            .reversed()
                                            .thenComparing(
                                                    HostCveResponse
                                                            ::cvssScore,
                                                    Comparator.nullsLast(
                                                            Comparator.reverseOrder()
                                                    )
                                            )
                                            .thenComparing(
                                                    HostCveResponse
                                                            ::epssScore,
                                                    Comparator.nullsLast(
                                                            Comparator.reverseOrder()
                                                    )
                                            )
                                            .thenComparing(
                                                    HostCveResponse
                                                            ::cveId
                                            )
                            )
                            .toList();

            return new HostVulnerabilidadResponse(
                    ip,
                    hostname,
                    all.size(),
                    criticas,
                    cvssMaximo,
                    epssMaximo,
                    kev,
                    cvesCriticas,
                    cvesPrioritarias
            );
        }
    }

    /* ============================================================
     * Host-CVE accumulator
     * ============================================================ */

    private static class HostCveAccumulator {

        private final String cveId;
        private BigDecimal cvss;
        private BigDecimal epss;
        private boolean kev;
        private final Set<String> cpes = new TreeSet<>();

        private HostCveAccumulator(String cveId) {
            this.cveId = cveId;
        }

        private void merge(
                com.tup.reconac.modules.vulnEnum.models.Cve cve,
                String cpe
        ) {

            if (cve.getCvss() != null) {

                if (cvss == null ||
                        cve.getCvss()
                                .compareTo(cvss) > 0) {

                    cvss =
                            cve.getCvss();
                }
            }

            if (cve.getEpss() != null) {

                if (epss == null ||
                        cve.getEpss()
                                .compareTo(epss) > 0) {

                    epss =
                            cve.getEpss();
                }
            }

            kev |=
                    Boolean.TRUE.equals(
                            cve.getKev()
                    );

            if (cpe != null &&
                    !cpe.isBlank()) {

                cpes.add(
                        cpe.trim()
                );
            }
        }

        private HostCveResponse toResponse() {

            return new HostCveResponse(
                    cveId,
                    cvss,
                    epss,
                    kev,
                    List.copyOf(cpes)
            );
        }
    }
}
