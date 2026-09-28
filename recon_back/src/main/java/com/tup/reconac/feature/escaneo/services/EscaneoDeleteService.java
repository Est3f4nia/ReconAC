package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoDeleteService;
import com.tup.reconac.feature.puerto.repositories.PuertoRepository;
import com.tup.reconac.modules.vulnEnum.repositories.PuertoCpeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EscaneoDeleteService implements IEscaneoDeleteService {

    private final EscaneoRepository escaneoRepository;
    private final ActivoRepository activoRepository;
    private final PuertoRepository puertoRepository;
    private final PuertoCpeRepository puertoCpeRepository;
    private final AuditoriaConsultService auditoriaConsult;

    @Override
    @Transactional
    public void eliminar(UUID escaneoId) {

        Escaneo escaneo = escaneoRepository.findById(escaneoId).orElseThrow(() ->
                new EscaneoNotFoundException("Escaneo no encontrado: " + escaneoId));

        auditoriaConsult.verifyAuditoriaOwnership(escaneo.getAuditoriaId());

        deleteActivos(escaneoId);
        escaneoRepository.delete(escaneo);
    }

    // Internal -----

    @Transactional
    public void eliminarAutorizado(UUID escaneoId) {

        deleteActivos(escaneoId);
        escaneoRepository.deleteById(escaneoId);

    }


    private void deleteActivos(UUID escaneoId) {
        List<UUID> activoIds = activoRepository.findIdsByEscaneoId(escaneoId);

        if (activoIds.isEmpty()) return;

        List<UUID> puertoIds = puertoRepository.findIdsByActivoIdIn(activoIds);

        if (!puertoIds.isEmpty()) {
            puertoCpeRepository.deleteByPuertoIdIn(puertoIds);
        }

        puertoRepository.deleteByActivoIds(activoIds);
        activoRepository.deleteAllByIdInBatch(activoIds);
    }
}

