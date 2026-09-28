package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaDeleteService;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.escaneo.services.EscaneoDeleteService;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditoriaDeleteService implements IAuditoriaDeleteService {

    private final AuditoriaRepository auditoriaRepository;
    private final EscaneoRepository escaneoRepository;
    private final EscaneoDeleteService escaneoDelete;
    private final AuditoriaConsultService auditoriaConsult;
    private final EscaneoConsultService escaneoConsult;

    @Override
    @Transactional
    public void deleteById(UUID auditoriaId) {

        auditoriaConsult.verifyAuditoriaOwnership(auditoriaId);

        List<UUID> escaneoIds = escaneoConsult.getEscaneoIds(auditoriaId);

        for (UUID escaneoId : escaneoIds) {
            escaneoDelete.eliminarAutorizado(escaneoId);
        }

        escaneoRepository.flush();
        auditoriaRepository.deleteById(auditoriaId);
    }
}
