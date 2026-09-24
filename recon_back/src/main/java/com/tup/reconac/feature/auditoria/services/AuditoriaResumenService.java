package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResumenResponseDto;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.feature.puerto.repositories.PuertoRepository;
import com.tup.reconac.feature.usuario.services.domain.CurrentUserService;
import com.tup.reconac.modules.vulnEnum.dtos.metricas.ScanMetrics;
import com.tup.reconac.modules.vulnEnum.services.metricas.ScanMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaResumenService {

    private final CurrentUserService currentUserService;
    private final AuditoriaConsultService auditoriaConsult;
    private final EscaneoRepository escaneoRepository;
    private final ActivoRepository activoRepository;
    private final PuertoRepository puertoRepository;
    private final ScanMetricsService scanMetricsService;

    public List<AuditoriaResumenResponseDto> getResumen() {

        List<Auditoria> auditorias = auditoriaConsult.findAllByUsuarioId(currentUserService.getUsuarioId());
        if (auditorias.isEmpty()) return List.of();

        List<UUID> auditoriaIds = auditorias
                .stream()
                .map(Auditoria::getId)
                .toList();

        List<Escaneo> escaneos = escaneoRepository
                .findByAuditoriaIdIn(auditoriaIds, Pageable
                        .unpaged(Sort.by(Sort.Direction.DESC, "creadoA"))
                )
                .getContent();

        Map<UUID, Escaneo> ultimoPorAuditoria = new LinkedHashMap<>();
        escaneos.forEach(escaneo ->
                ultimoPorAuditoria.putIfAbsent(escaneo.getAuditoriaId(), escaneo)
        );

        List<UUID> escaneoIds = ultimoPorAuditoria
                .values().stream()
                .map(Escaneo::getId)
                .toList();

        List<Activo> activos = escaneoIds.isEmpty()
                ? List.of()
                : activoRepository.findByEscaneoIdIn(escaneoIds);

        Map<UUID, List<Activo>> activosPorEscaneo = activos.stream()
                .collect(Collectors.groupingBy(Activo::getEscaneoId));

        List<UUID> activoIds = activos
                .stream()
                .map(Activo::getId)
                .toList();

        Map<UUID, Long> puertosPorActivo = activoIds.isEmpty()
                ? Map.of()
                : puertoRepository.findByActivoIdIn(activoIds).stream()
                .collect(Collectors.groupingBy(
                        Puerto::getActivoId,
                        Collectors.counting()
                ));

        return auditorias.stream()
                .map(auditoria -> {
                    Escaneo escaneo = ultimoPorAuditoria.get(auditoria.getId());

                    if (escaneo == null) {
                        return AuditoriaMapper.toResumenResponse(auditoria);
                    }

                    ScanMetrics metrics = scanMetricsService.calculate(
                            escaneo,
                            activosPorEscaneo,
                            puertosPorActivo
                    );

                    return AuditoriaMapper.toResumenResponse(
                            auditoria,
                            escaneo,
                            Math.toIntExact(metrics.activos()),
                            Math.toIntExact(metrics.puertos()),
                            Math.toIntExact(metrics.cves()),
                            Math.toIntExact(metrics.cvesCriticos())
                    );
                })
                .toList();
    }
}

//    // DTO interno
//
//    private record ResumenMetricas(int activos, int puertos, int cves, int cvesCriticos) {
//        private static ResumenMetricas empty() {
//            return new ResumenMetricas(0, 0, 0, 0);
//        }
//    }
//}