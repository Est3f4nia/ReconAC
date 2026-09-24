package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.feature.activo.dtos.response.ActivoResultadoResponse;
import com.tup.reconac.feature.activo.mappers.ActivoMapper;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.mappers.EscaneoMapper;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoGetService;
import com.tup.reconac.feature.puerto.dtos.PuertoResultadoResponse;
import com.tup.reconac.feature.puerto.mapper.PuertoMapper;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.feature.puerto.repositories.PuertoRepository;
import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.models.PuertoCpe;
import com.tup.reconac.modules.vulnEnum.repositories.CpeRepository;
import com.tup.reconac.modules.vulnEnum.repositories.PuertoCpeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EscaneoGetService implements IEscaneoGetService {

    private final EscaneoConsultService escaneoConsult;
    private final ActivoRepository activoRepo;
    private final PuertoRepository puertoRepo;
    private final PuertoCpeRepository puertoCpeRepo;
    private final CpeRepository cpeRepo;

    @Override
    @Transactional(readOnly = true)
    public ScanStatusResponse getStatus(UUID auditoriaId, UUID escaneoId) {

        Escaneo escaneo = escaneoConsult.findEscaneoForAuditoria(auditoriaId, escaneoId);
        return EscaneoMapper.toStatusResponse(escaneo);
    }

    @Override
    @Transactional(readOnly = true)
    public EscaneoResult.EscaneoResultResponse getResultado(UUID auditoriaId, UUID escaneoId) {

        Escaneo escaneo = escaneoConsult.findEscaneoForAuditoria(auditoriaId, escaneoId);
        return buildResultado(escaneo);
    }

    @Override
    @Transactional(readOnly = true)
    public EscaneoResult.EscaneoResultResponse getById(UUID escaneoId) {

        Escaneo escaneo = escaneoConsult.findEscaneoForUsuario(escaneoId);

        return buildResultado(escaneo);
    }

    private EscaneoResult.EscaneoResultResponse buildResultado(Escaneo escaneo) {

        List<Activo> activos = activoRepo.findByEscaneoId(escaneo.getId());

        if (activos.isEmpty()) {
            return EscaneoMapper.toResultResponse(escaneo, List.of());
        }

        List<UUID> activoIds = activos.stream()
                .map(Activo::getId)
                .toList();

        List<Puerto> puertos = puertoRepo.findByActivoIdIn(activoIds);

        Map<UUID, List<Puerto>> puertosPorActivo = puertos.stream()
                .collect(Collectors.groupingBy(Puerto::getActivoId));

        Map<UUID, List<String>> cpesPorPuerto = loadCpesByPuerto(puertos);

        List<ActivoResultadoResponse> activosResponse = activos.stream()
                .map(activo -> {
                    List<PuertoResultadoResponse> puertosResponse = puertosPorActivo
                            .getOrDefault(activo.getId(), List.of())
                            .stream()
                            .map(puerto -> PuertoMapper.toResultadoResponse(
                                    puerto,
                                    cpesPorPuerto.getOrDefault(puerto.getId(), List.of())
                            ))
                            .toList();

                    return ActivoMapper.toResultadoResponse(activo, puertosResponse);
                })
                .toList();

        return EscaneoMapper.toResultResponse(escaneo, activosResponse);
    }

    private Map<UUID, List<String>> loadCpesByPuerto(List<Puerto> puertos) {

        if (puertos.isEmpty()) return Map.of();

        List<UUID> puertoIds = puertos.stream()
                .map(Puerto::getId)
                .toList();

        List<PuertoCpe> relaciones = puertoCpeRepo.findByPuertoIdIn(puertoIds);

        if (relaciones.isEmpty()) return Map.of();

        Set<UUID> cpeIds = relaciones.stream()
                .map(PuertoCpe::getCpeId)
                .collect(Collectors.toSet());

        Map<UUID, String> cpeUris = cpeRepo.findAllById(cpeIds).stream()
                .collect(Collectors.toMap(
                        Cpe::getId,
                        Cpe::getUri
                ));

        Map<UUID, List<String>> result = new HashMap<>();

        for (PuertoCpe relacion : relaciones) {
            String uri = cpeUris.get(relacion.getCpeId());
            if (uri != null) {
                result.computeIfAbsent(relacion.getPuertoId(), id -> new ArrayList<>()).add(uri);
            }
        }

        return result;
    }
}
