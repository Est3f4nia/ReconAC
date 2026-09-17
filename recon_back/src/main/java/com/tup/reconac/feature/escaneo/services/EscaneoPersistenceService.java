package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Transacción aparte para persistir el escaneo.
 * Al crear es el escaneo E iniciar Python,
 * @Transactional no persistía el moduleJobId a tiempo para que las
 * actualizaciones de estado funcionen
 */

@Service
@AllArgsConstructor
public class EscaneoPersistenceService {

    private final EscaneoRepository repo;
    private final AuditoriaConsultService auditoriaConsult;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Escaneo crear(UUID auditoriaId,  String[] objetivos,  String jobId) {
        auditoriaConsult.verifyAuditoriaOwnership(auditoriaId);

        Escaneo escaneo = new Escaneo();

        escaneo.setAuditoriaId(auditoriaId);
        escaneo.setObjetivos(objetivos);
        escaneo.setModuloJobId(jobId);
        escaneo.setEstado(EscaneoEstado.PENDIENTE);

        return repo.saveAndFlush(escaneo);
    }
}
