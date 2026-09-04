package com.tup.reconac.feature.auditoria.services.domain;

import com.tup.reconac.exceptions.auditoria.AuditoriaNotFoundException;
import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuditoriaConsultService {

    private final AuditoriaRepository repo;

    public Auditoria findId(UUID auditoriaId) {
        return repo.findById(auditoriaId)
                .orElseThrow(() -> new AuditoriaNotFoundException("Auditoría no encontrada: " + auditoriaId));
    }

    public void escaneos(Escaneo escaneo, UUID usuarioId) {
        repo.findByIdAndUsuarioId(escaneo.getAuditoriaId(), usuarioId)
                .orElseThrow(() ->
                        new EscaneoNotFoundException("Escaneo no encontrado")
                );
    }


    public List<Auditoria> findAllByUsuarioId(UUID usuarioId) {
        return repo.findByUsuarioIdOrderByFechaGeneracionDesc(usuarioId);
    }
}
