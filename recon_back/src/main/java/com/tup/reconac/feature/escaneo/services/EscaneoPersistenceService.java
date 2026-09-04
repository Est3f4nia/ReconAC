package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Transacción aparte para persistir el escaneo.
 * @Transactional no persistía el moduleJobId a tiempo para que las
 * actualizaciones de estado funcionen
 */

@Service
@AllArgsConstructor
public class EscaneoPersistenceService {
    private final EscaneoRepository repo;

    @Transactional
    public Escaneo crear(
            UUID auditoriaId,
            String[] objetivos,
            String jobId) {

        Escaneo escaneo = new Escaneo();

        escaneo.setAuditoriaId(auditoriaId);
        escaneo.setObjetivos(objetivos);
        escaneo.setModuloJobId(jobId);
        escaneo.setEstado(EscaneoEstado.PENDIENTE);

        return repo.save(escaneo);
    }
}
