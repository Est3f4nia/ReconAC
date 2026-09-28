package com.tup.reconac.modules.vulnEnum.services.metricas;

import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.feature.puerto.repositories.PuertoRepository;
import com.tup.reconac.modules.vulnEnum.dtos.metricas.*;
import com.tup.reconac.modules.vulnEnum.mappers.MetricasMapper;
import com.tup.reconac.modules.vulnEnum.services.interfaces.IMetricasAuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricasAuditoriaService implements IMetricasAuditoriaService {

    private final EscaneoRepository escaneoRepository;
    private final ActivoRepository activoRepository;
    private final PuertoRepository puertoRepository;
    private final AuditoriaConsultService auditoriaConsult;
    private final ScanMetricsService scanMetricsService;
    private final CveDesgloseService cveDesgloseService;
    private final HostDesgloseService hostDesgloseService;

    @Override
    public DashboardAuditoriaResponse getDashboard(UUID auditoriaId) {

        auditoriaConsult.verifyAuditoriaOwnership(auditoriaId);

        List<Escaneo> escaneos = escaneoRepository.findByAuditoriaIdOrderByCreadoADesc(auditoriaId);

        if (escaneos.isEmpty()) {
            return MetricasMapper.emptyDashboard();
        }

        Map<UUID, Escaneo> escaneosPorId = escaneos
                .stream()
                .collect(Collectors.toMap(
                        Escaneo::getId,
                        escaneo -> escaneo
                ));

        List<UUID> escaneoIds = escaneos
                .stream()
                .map(Escaneo::getId)
                .toList();

        List<Activo> activos = activoRepository.findByEscaneoIdIn(escaneoIds);

        Map<UUID, List<Activo>> activosPorEscaneo = activos
                .stream()
                .collect(Collectors.groupingBy(Activo::getEscaneoId));

        List<UUID> activoIds = activos
                .stream()
                .map(Activo::getId)
                .toList();

        List<Puerto> puertos = activoIds.isEmpty()
                ? List.of()
                : puertoRepository.findByActivoIdIn(activoIds);

        Map<UUID, Long> puertosPorActivo = puertos
                .stream()
                .collect(Collectors.groupingBy(Puerto::getActivoId,
                        Collectors.counting()
                ));

        List<ScanMetrics> metrics = escaneos
                .stream()
                .map(escaneo -> scanMetricsService.calculate(
                        escaneo,
                        activosPorEscaneo,
                        puertosPorActivo
                ))
                .toList();

        CveDesgloseResponse vulnerabilidades = cveDesgloseService.build(metrics, activos, puertos);

        long cvesExplotados = vulnerabilidades.explotacionActiva() == null
                        ? 0
                        : vulnerabilidades.explotacionActiva().size();

        DashboardKpisResponse kpis =
                scanMetricsService.calculateKpis(
                        escaneos,
                        activos,
                        metrics,
                        cvesExplotados
                );

        List<EjecucionHistorialResponse> historial = metrics
                .stream()
                .map(metric -> MetricasMapper.toHistorial(
                        metric,
                        escaneosPorId.get(metric.escaneoId())
                ))
                .toList();

        List<RiesgoTemporalResponse> riesgoTemporal = metrics
                .stream()
                .map(MetricasMapper::toRiesgoTemporal)
                .toList();

        HostDesgloseResponse hosts = hostDesgloseService.build(activos, puertos);

        return new DashboardAuditoriaResponse(
                kpis,
                historial,
                riesgoTemporal,
                vulnerabilidades,
                hosts
        );
    }
}