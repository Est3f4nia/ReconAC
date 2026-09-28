package com.tup.reconac.modules.vulnEnum.services.metricas;

import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.modules.vulnEnum.dtos.metricas.HostCveResponse;
import com.tup.reconac.modules.vulnEnum.dtos.metricas.HostDesgloseResponse;
import com.tup.reconac.modules.vulnEnum.dtos.metricas.HostVulnerabilidadResponse;
import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.models.CpeCve;
import com.tup.reconac.modules.vulnEnum.models.Cve;
import com.tup.reconac.modules.vulnEnum.models.PuertoCpe;
import com.tup.reconac.modules.vulnEnum.repositories.CpeCveRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CpeRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CveRepository;
import com.tup.reconac.modules.vulnEnum.repositories.PuertoCpeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HostDesgloseService {

    private final PuertoCpeRepository puertoCpeRepository;
    private final CpeRepository cpeRepository;
    private final CpeCveRepository cpeCveRepository;
    private final CveRepository cveRepository;

    public HostDesgloseResponse build(List<Activo> activos, List<Puerto> puertos) {

            if (activos.isEmpty() || puertos.isEmpty()) {
                return new HostDesgloseResponse(List.of(), List.of(), List.of());
            }

            Map<UUID, Activo> activosPorId = activos
                    .stream()
                    .collect(Collectors.toMap(Activo::getId,activo -> activo));

            List<UUID> puertoIds = puertos
                    .stream()
                    .map(Puerto::getId)
                    .toList();

            /*
             * ========================================================
             * Puerto -> CPE
             * ========================================================
             */

            List<PuertoCpe> puertoCpes = puertoIds.isEmpty()
                    ? List.of()
                    : puertoCpeRepository
                    .findByPuertoIdIn(puertoIds);

            if (puertoCpes.isEmpty()) {
                return new HostDesgloseResponse(List.of(), List.of(), List.of());
            }

            Set<UUID> cpeIds = puertoCpes
                    .stream()
                    .map(PuertoCpe::getCpeId)
                    .collect(Collectors.toSet());

            Map<UUID, Cpe> cpesPorId = cpeRepository
                            .findAllById(cpeIds)
                            .stream()
                            .collect(Collectors.toMap(Cpe::getId, cpe -> cpe));

            /*
             * ========================================================
             * CPE -> CVE
             * ========================================================
             */

            List<CpeCve> cpeCves = cpeIds
                    .isEmpty()
                    ? List.of()
                    : cpeCveRepository
                    .findByCpeIdIn(cpeIds);

            if (cpeCves.isEmpty()) {
                return new HostDesgloseResponse(List.of(), List.of(), List.of());
            }

            Set<UUID> cveIds = cpeCves
                    .stream()
                    .map(CpeCve::getCveId)
                    .collect(Collectors.toSet());



            Map<UUID, Cve> cvesPorId = cveRepository
                    .findAllById(cveIds)
                    .stream()
                    .collect(Collectors.toMap(Cve::getId, cve -> cve));

            /*
             * ========================================================
             * CPE -> CVEs
             * ========================================================
             */

            Map<UUID, List<CpeCve>> cvesPorCpe = cpeCves
                    .stream()
                    .collect(Collectors.groupingBy(CpeCve::getCpeId));

            /*
             * ========================================================
             * Puerto -> CPEs
             * ========================================================
             */

            Map<UUID, List<PuertoCpe>> cpesPorPuerto = puertoCpes
                    .stream()
                    .collect(Collectors.groupingBy(PuertoCpe::getPuertoId));


            Map<String, HostAccumulator> hosts = new LinkedHashMap<>();

            for (Puerto puerto : puertos) {

                Activo activo = activosPorId.get(puerto.getActivoId());
                if (activo == null || activo.getHost() == null || activo.getHost().isBlank()) continue;

                String hostIp = activo.getHost().trim();

                HostAccumulator host = hosts
                        .computeIfAbsent( hostIp, ignored ->
                                new HostAccumulator( hostIp, activo.getHostname())
                        );

                if ((host.hostname == null || host.hostname.isBlank()) && activo.getHostname() != null
                        && !activo.getHostname().isBlank()) {
                    host.hostname = activo.getHostname();
                }

                List<PuertoCpe> relaciones = cpesPorPuerto.getOrDefault(puerto.getId(), List.of());
                String puertoLabel = formatPuerto(puerto);

                for (PuertoCpe puertoCpe : relaciones) {

                    UUID cpeId = puertoCpe.getCpeId();

                    Cpe cpe = cpesPorId.get(cpeId);
                    if (cpe == null) continue;

                    List<CpeCve> relacionesCve = cvesPorCpe.getOrDefault(cpeId, List.of());

                    for (CpeCve cpeCve : relacionesCve) {

                        Cve cve = cvesPorId.get(cpeCve.getCveId());

                        if (cve == null || cve.getCve() == null) continue;

                        host.addCve(cve, cpe.getUri(), puertoLabel);
                    }
                }
            }


            List<HostVulnerabilidadResponse> all = hosts
                    .values()
                    .stream()
                    .filter(host -> !host.cves.isEmpty())
                    .map(HostAccumulator::toResponse)
                    .toList();


            List<HostVulnerabilidadResponse> masVulnerabilidadesCriticas = all
                    .stream()
                    .filter(host -> host.vulnerabilidadesCriticas() > 0)
                    .sorted(Comparator
                                    .comparingLong(HostVulnerabilidadResponse::vulnerabilidadesCriticas)
                                    .reversed()
                                    .thenComparing(HostVulnerabilidadResponse::cvssMaximo,
                                            Comparator.nullsLast(Comparator.reverseOrder())
                                    )
                                    .thenComparing(HostVulnerabilidadResponse::ip)
                    )
                    .limit(10)
                    .toList();

            /*
             * ========================================================
             * KEV > CVSS > EPSS
             * ========================================================
             */

            List<HostVulnerabilidadResponse> mayorRiesgoExplotacion = all
                    .stream()
                    .sorted(Comparator
                                    .comparing(HostVulnerabilidadResponse::kev
                                    )
                                    .reversed()
                                    .thenComparing(HostVulnerabilidadResponse::cvssMaximo,
                                            Comparator.nullsLast(Comparator.reverseOrder())
                                    )
                                    .thenComparing(HostVulnerabilidadResponse::epssMaximo,
                                            Comparator.nullsLast(Comparator.reverseOrder())
                                    )
                                    .thenComparing(HostVulnerabilidadResponse::ip)
                    )
                    .limit(10)
                    .toList();

            return new HostDesgloseResponse(
                    all,
                    masVulnerabilidadesCriticas,
                    mayorRiesgoExplotacion);
    }

    private String formatPuerto(Puerto puerto) {

        String protocolo = puerto.getProtocolo() == null || puerto.getProtocolo().isBlank()
                ? null
                : puerto.getProtocolo()
                .trim()
                .toLowerCase(Locale.ROOT);

        return protocolo == null
                ? String.valueOf(puerto.getNumero())
                : puerto.getNumero() + "/" + protocolo;
    }

    private static class HostAccumulator {

        private final String ip;
        private String hostname;
        private final Map<String, HostCveAccumulator> cves = new HashMap<>();

        private HostAccumulator(String ip, String hostname) {
            this.ip = ip;
            this.hostname = hostname;
        }


        private void addCve(Cve cve, String cpe, String puerto) {

            String cveId = cve
                    .getCve()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            HostCveAccumulator current = cves
                    .computeIfAbsent(cveId, ignored -> new HostCveAccumulator(cveId)
            );

            current.merge(cve, cpe, puerto);
        }

        private HostVulnerabilidadResponse toResponse() {

            List<HostCveResponse> all = cves
                    .values()
                    .stream()
                    .map( HostCveAccumulator::toResponse)
                    .toList();

            long criticas = all
                    .stream()
                    .filter(cve ->
                                    cve.cvssScore() != null && cve.cvssScore()
                                            .compareTo(BigDecimal.valueOf(9)) >= 0
                            )
                    .count();

            BigDecimal cvssMaximo = all
                    .stream()
                    .map(HostCveResponse::cvssScore)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(null);

            BigDecimal epssMaximo = all
                    .stream()
                    .map(HostCveResponse::epssScore)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(null);

            boolean kev = all
                    .stream()
                    .anyMatch(HostCveResponse::kev);

            List<HostCveResponse> cvesCriticas = all
                    .stream()
                    .filter(cve -> cve.cvssScore() != null && cve.cvssScore()
                                    .compareTo(BigDecimal.valueOf(9)) >= 0
                            )
                    .sorted(Comparator
                                    .comparing(HostCveResponse::cvssScore,
                                            Comparator.nullsLast(Comparator.reverseOrder())
                                    )
                                    .thenComparing(HostCveResponse::cveId)
                    )
                    .toList();

            /*
             * KEV > CVSS > EPSS.
             */

            List<HostCveResponse> cvesPrioritarias = all
                    .stream()
                    .sorted(Comparator
                                    .comparing(HostCveResponse::kev)
                                    .reversed()
                                    .thenComparing(HostCveResponse::cvssScore,
                                            Comparator.nullsLast(Comparator.reverseOrder())
                                    )
                                    .thenComparing(HostCveResponse::epssScore,
                                            Comparator.nullsLast(Comparator.reverseOrder())
                                    )
                                    .thenComparing(HostCveResponse::cveId)
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
                    cvesPrioritarias,
                    all
            );
        }
    }

    private static class HostCveAccumulator {

        private final String cveId;
        private BigDecimal cvss;
        private BigDecimal epss;
        private boolean kev;
        private final Set<String> cpes = new TreeSet<>();
        private final Set<String> puertos = new TreeSet<>();

        private HostCveAccumulator(String cveId) {
            this.cveId = cveId;
        }

        private void merge(Cve cve, String cpe, String puerto) {

            if (cve.getCvss() != null) {
                if (cvss == null || cve.getCvss().compareTo(cvss) > 0) cvss = cve.getCvss();
            }

            if (cve.getEpss() != null) {
                if (epss == null || cve.getEpss().compareTo(epss) > 0) epss = cve.getEpss();
            }

            kev |= Boolean.TRUE.equals(cve.getKev());

            if (cpe != null && !cpe.isBlank()) cpes.add(cpe.trim());

            if (puerto != null && !puerto.isBlank()) puertos.add(puerto.trim());
        }

        private HostCveResponse toResponse() {
            return new HostCveResponse(cveId, cvss, epss, kev, List.copyOf(cpes), List.copyOf(puertos));
        }
    }
}

