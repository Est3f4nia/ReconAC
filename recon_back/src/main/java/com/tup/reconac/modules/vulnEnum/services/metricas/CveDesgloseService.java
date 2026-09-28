package com.tup.reconac.modules.vulnEnum.services.metricas;

import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.modules.vulnEnum.dtos.data.CveData;
import com.tup.reconac.modules.vulnEnum.dtos.metricas.CveDesgloseResponse;
import com.tup.reconac.modules.vulnEnum.dtos.metricas.CveResumenResponse;
import com.tup.reconac.modules.vulnEnum.dtos.metricas.ScanMetrics;
import com.tup.reconac.modules.vulnEnum.models.CpeCve;
import com.tup.reconac.modules.vulnEnum.models.Cve;
import com.tup.reconac.modules.vulnEnum.models.PuertoCpe;
import com.tup.reconac.modules.vulnEnum.repositories.CpeCveRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CveRepository;
import com.tup.reconac.modules.vulnEnum.repositories.PuertoCpeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CveDesgloseService {

    private final PuertoCpeRepository puertoCpeRepository;
    private final CpeCveRepository cpeCveRepository;
    private final CveRepository cveRepository;

    public CveDesgloseResponse build(List<ScanMetrics> metrics, List<Activo> activos, List<Puerto> puertos) {

        Map<String, CveAccumulator> accumulator = new HashMap<>();

        for (ScanMetrics metric : metrics) {
            for (CveData cve : metric.cvesData()) {

                if (cve.id() == null) continue;

                CveAccumulator current = accumulator.computeIfAbsent(
                        cve.id(),
                        CveAccumulator::new
                );

                current.frecuencia++;

                if (cve.cvss() != null && (current.cvss == null || cve.cvss().compareTo(current.cvss) > 0)) {
                    current.cvss = cve.cvss();
                }

                if (cve.epss() != null && (current.epss == null || cve.epss().compareTo(current.epss) > 0)) {
                    current.epss = cve.epss();
                }


                current.explotacionActiva |= cve.explotacionActiva();

                current.cwes.addAll(cve.cwes());
            }
        }


        Map<UUID, UUID> escaneoPorActivo = activos.stream()
                .collect(
                        Collectors.toMap(
                                Activo::getId,
                                Activo::getEscaneoId
                        )
                );

        Map<UUID, UUID> activoPorPuerto = puertos
                .stream()
                .collect(
                        Collectors.toMap(
                                Puerto::getId,
                                Puerto::getActivoId
                        )
                );

        List<UUID> puertoIds = puertos
                .stream()
                .map(Puerto::getId)
                .toList();

        List<PuertoCpe> puertoCpes = puertoIds
                .isEmpty()
                ? List.of()
                : puertoCpeRepository.findByPuertoIdIn(puertoIds);

        Map<UUID, Set<UUID>> escaneosPorCpe = new HashMap<>();

        for (PuertoCpe relacion : puertoCpes) {
            UUID activoId = activoPorPuerto.get(relacion.getPuertoId());

            if (activoId == null) continue;

            UUID escaneoId = escaneoPorActivo.get(activoId);

            if (escaneoId == null) continue;

            escaneosPorCpe.computeIfAbsent(
                    relacion.getCpeId(),
                            ignored -> new HashSet<>()
                    )
                    .add(escaneoId);
        }

        Set<UUID> cpeIds = puertoCpes
                .stream()
                .map(PuertoCpe::getCpeId)
                .collect(Collectors.toSet());

        List<CpeCve> cpeCves = cpeIds
                .isEmpty()
                ? List.of()
                : cpeCveRepository.findByCpeIdIn(cpeIds);

        Set<UUID> cveEntityIds = cpeCves
                .stream()
                .map(CpeCve::getCveId)
                .collect(Collectors.toSet());

        Map<UUID, Cve> cvesPorId = cveRepository
                .findAllById(cveEntityIds)
                .stream()
                .collect(Collectors.toMap(
                        Cve::getId,
                                cve -> cve
                        )
                );

        /*
         * evita contar varias veces una CVE por estar asociada
         * a varios puertos/CPE dentro del mismo escaneo.
         */

        Map<String, Set<UUID>> escaneosPorCve = new HashMap<>();

        for (CpeCve relacion : cpeCves) {
            var saved = cvesPorId.get(relacion.getCveId());

            if (saved == null || saved.getCve() == null || saved.getCve().isBlank()) continue;

            String cveId = saved
                    .getCve()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            CveAccumulator current = accumulator.computeIfAbsent(
                    cveId,
                    CveAccumulator::new
            );

            if (saved.getCvss() != null && (current.cvss == null || saved.getCvss().compareTo(current.cvss) > 0)) {
                current.cvss = saved.getCvss();
            }

            if (saved.getEpss() != null && (current.epss == null || saved.getEpss().compareTo(current.epss) > 0)) {
                current.epss = saved.getEpss();
            }

            current.explotacionActiva |= Boolean.TRUE.equals(saved.getKev());

            Set<UUID> scanIds = escaneosPorCpe
                    .getOrDefault(
                            relacion.getCpeId(),
                            Set.of()
                    );

            escaneosPorCve.computeIfAbsent(
                    cveId,
                            ignored -> new HashSet<>()
                    )
                    .addAll(scanIds);
        }

        for (Map.Entry<String, Set<UUID>> entry : escaneosPorCve.entrySet()) {
            CveAccumulator current = accumulator.get(entry.getKey());

            if (current != null) {
                current.frecuencia = Math.max(
                        current.frecuencia,
                        entry.getValue().size()
                );
            }
        }

        List<CveResumenResponse> all = accumulator
                .values()
                .stream()
                .map(CveAccumulator::toResponse)
                .toList();

        List<CveResumenResponse> masComunes = all
                .stream()
                .sorted(
                        Comparator.comparingLong(
                                CveResumenResponse::frecuencia)
                                .reversed()
                )
                .limit(10)
                .toList();

        List<CveResumenResponse> explotacionActiva = all
                .stream()
                .filter(CveResumenResponse::explotacionActiva)
                .sorted(
                        Comparator.comparing(
                                CveResumenResponse::cvssScore,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .limit(10)
                .toList();

        List<CveResumenResponse> mayorCriticidad = all
                .stream()
                .sorted(
                        Comparator.comparing(
                                CveResumenResponse::cvssScore,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                ).thenComparing(
                                        cve -> getCveYear(cve.cveId()),
                                        Comparator.reverseOrder()
                                )
                )
                .limit(10)
                .toList();

        List<CveResumenResponse> mayorProbabilidadExplotacion = all
                .stream()
                .filter(cve -> cve.epssScore() != null)
                .sorted(
                        Comparator.comparing(
                                CveResumenResponse::epssScore,
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

    private int getCveYear(String cveId) {

        if (cveId == null || !cveId.startsWith("CVE-")) return 0;

        try {
            return Integer.parseInt(cveId.split("-")[1]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    private static class CveAccumulator {

        private final String id;
        private long frecuencia;
        private BigDecimal cvss;
        private boolean explotacionActiva;
        private BigDecimal epss;

        private final Set<String> cwes = new HashSet<>();

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
                    cwes.stream().sorted().toList()
            );
        }
    }
}
