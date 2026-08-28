package com.tup.reconac.feature.auditoria.services.domain;

import com.tup.reconac.exceptions.auditoria.AuditoriaNotFoundException;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AuditoriaConsultService {

    private final AuditoriaRepository repo;

    public Auditoria findId(UUID auditoriaId) {
        return repo.findById(auditoriaId)
                .orElseThrow(() -> new AuditoriaNotFoundException("Auditoría no encontrada: " + auditoriaId));
    }
}
