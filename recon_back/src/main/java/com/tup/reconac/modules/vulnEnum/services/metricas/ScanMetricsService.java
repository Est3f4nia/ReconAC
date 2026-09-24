package com.tup.reconac.modules.vulnEnum.services.metricas;

import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.modules.vulnEnum.dtos.data.CveData;
import com.tup.reconac.modules.vulnEnum.dtos.metricas.DashboardKpisResponse;
import com.tup.reconac.modules.vulnEnum.dtos.metricas.ScanMetrics;
import com.tup.reconac.modules.vulnEnum.models.Cve;
import com.tup.reconac.modules.vulnEnum.repositories.CveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScanMetricsService {

    private final ObjectMapper objectMapper;
    private final CveRepository cveRepository;

    public ScanMetrics calculate(Escaneo escaneo, Map<UUID, List<Activo>> activosPorEscaneo,
                                 Map<UUID, Long> puertosPorActivo) {

        List<Activo> activos = activosPorEscaneo.getOrDefault(escaneo.getId(), List.of());

        long puertos = activos
                .stream()
                .mapToLong(activo ->
                        puertosPorActivo.getOrDefault(activo.getId(), 0L)
                )
                .sum();

        if (escaneo.getResultado() == null || escaneo.getResultado().isBlank()) {
            return ScanMetrics.empty(escaneo, activos.size(), puertos);
        }

        try {
            JsonNode root = objectMapper.readTree(escaneo.getResultado());
            JsonNode vulnerabilities = vulnerabilityNodes(root.path("apiResults"));
            List<CveData> cvesData = new ArrayList<>();

            if (vulnerabilities.isArray()) {

                for (JsonNode vulnerability : vulnerabilities) {

                    String cveId = vulnerability
                            .path("cve_id")
                            .asString(null);

                    if (cveId == null || cveId.isBlank()) continue;

                    cveId = normalizeCveId(cveId);
                    BigDecimal cvss = decimal(vulnerability);
                    Set<String> cwes = new HashSet<>();
                    JsonNode cweNodes = vulnerability.path("cwes");

                    if (cweNodes.isArray()) {

                        for (JsonNode cwe : cweNodes) {

                            String id = cwe
                                    .path("id")
                                    .asString(null);

                            if (id != null && !id.isBlank()) cwes.add(id);
                        }
                    }

                    cvesData.add(new CveData(
                            cveId,
                            cvss,
                            null,
                            false,
                            cwes.stream().sorted().toList()
                            )
                    );
                }
            }

            /*
             * un mismo CVE puede aparecer varias veces por afectar distintos hosts/servicios.
             */

            cvesData = deduplicateCves(cvesData);

            List<String> cveIds = cvesData.stream()
                    .map(CveData::id)
                    .filter(Objects::nonNull)
                    .map(this::normalizeCveId)
                    .distinct()
                    .toList();

            Map<String, Cve> catalog = cveIds.isEmpty()
                    ? Map.of()
                    : cveRepository.findAllByCveIn(cveIds)
                    .stream()
                    .filter(cve -> cve.getCve() != null)
                    .collect(Collectors.toMap(
                            cve -> normalizeCveId(cve.getCve()),
                            cve -> cve,
                            (actual, duplicado) -> actual
                    ));

            cvesData = cvesData.stream()
                    .map(cve -> {
                        Cve saved = catalog.get(normalizeCveId(cve.id()));

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

                    if (cvss.compareTo(BigDecimal.valueOf(9)) >= 0) criticos++;
                    else if (cvss.compareTo(BigDecimal.valueOf(7)) >= 0) altos++;
                    else if (cvss.compareTo(BigDecimal.valueOf(4)) >= 0) medios++;
                    else bajos++;
                }

                if (cve.explotacionActiva()) explotados++;
            }

            BigDecimal promedio = cvssCount == 0
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
                            : List.of(escaneo.getObjetivos()),
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
            throw new IllegalStateException(
                    "No se pudieron leer las métricas del escaneo " + escaneo.getId(), e
            );
        }
    }

    public DashboardKpisResponse calculateKpis(List<Escaneo> escaneos, List<Activo> activos, List<ScanMetrics> metrics, long cvesExplotados) {

        List<CveData> uniqueCveData = deduplicateCves(metrics
                .stream()
                .flatMap(metric -> metric.cvesData().stream())
                .toList()
        );

        long cves = uniqueCveData.size();

        long criticos = uniqueCveData
                .stream()
                .filter(cve ->
                        cve.cvss() != null && cve.cvss().compareTo(BigDecimal.valueOf(9)) >= 0
                )
                .count();

        long altos = uniqueCveData
                .stream()
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
                        cve.cvss() != null && cve.cvss().compareTo(BigDecimal.valueOf(4)) >= 0
                                && cve.cvss().compareTo(BigDecimal.valueOf(7)) < 0
                )
                .count();

        long bajos = uniqueCveData.stream()
                .filter(cve ->
                        cve.cvss() != null && cve.cvss().compareTo(BigDecimal.valueOf(4)) < 0
                )
                .count();

        List<BigDecimal> cvss = uniqueCveData
                .stream()
                .map(CveData::cvss)
                .filter(Objects::nonNull)
                .toList();

        BigDecimal cvssPromedio = null;

        if (!cvss.isEmpty()) {

            BigDecimal suma = cvss
                    .stream()
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );

            cvssPromedio = suma.divide(
                    BigDecimal.valueOf(cvss.size()),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        List<BigDecimal> epss = uniqueCveData
                .stream()
                .map(CveData::epss)
                .filter(Objects::nonNull)
                .toList();

        BigDecimal epssPromedio = null;

        if (!epss.isEmpty()) {

            BigDecimal suma = epss.stream()
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );

            epssPromedio = suma.divide(
                    BigDecimal.valueOf(epss.size()),
                    4,
                    RoundingMode.HALF_UP
            );
        }

        long puertos = metrics
                .stream()
                .mapToLong(ScanMetrics::puertos)
                .sum();

        long activosUnicos = activos
                .stream()
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
                cvesExplotados,
                epssPromedio
        );
    }

    private List<CveData> deduplicateCves(List<CveData> cves) {

        Map<String, CveData> unique = new HashMap<>();

        for (CveData cve : cves) {

            if (cve.id() == null) continue;

            String cveId = normalizeCveId(cve.id());

            CveData normalized = new CveData(
                    cveId,
                    cve.cvss(),
                    cve.epss(),
                    cve.explotacionActiva(),
                    cve.cwes() == null
                            ? List.of()
                            : cve.cwes()
            );

            unique.merge(cveId, normalized, this::mergeCveData);
        }

        return unique
                .values()
                .stream()
                .sorted(Comparator.comparing(CveData::id))
                .toList();
    }

    private CveData mergeCveData(CveData current, CveData incoming) {

        BigDecimal cvss;

        if (current.cvss() == null) cvss = incoming.cvss();
        else if (incoming.cvss() == null) cvss = current.cvss();
        else cvss = current.cvss().max(incoming.cvss());

        BigDecimal epss;

        if (current.epss() == null) epss = incoming.epss();
        else if (incoming.epss() == null) epss = current.epss();
        else epss = current.epss().max(incoming.epss());

        Set<String> cwes = new HashSet<>();

        if (current.cwes() != null) cwes.addAll(current.cwes());

        if (incoming.cwes() != null) cwes.addAll(incoming.cwes());

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

    private JsonNode vulnerabilityNodes(JsonNode apiResults) {

        if (apiResults.path("vulnerabilities").isArray()) {
            return apiResults.path("vulnerabilities");
        }

        var result = objectMapper.createArrayNode();

        for (JsonNode entry : apiResults) {

            if (!entry.path("vulnerabilities").isArray()) continue;

            for (JsonNode vulnerability : entry.path("vulnerabilities")) {
                result.add(vulnerability);
            }
        }

        return result;
    }

    private BigDecimal decimal(JsonNode node) {

        JsonNode value = node.get("cvss_score");

        if (value == null || !value.isNumber()) return null;

        return value
                .decimalValue()
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCveId(String id) {
        return id == null
                ? null
                : id.trim().toUpperCase(Locale.ROOT);
    }
}