package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.exceptions.auditoria.AuditoriaNotFoundException;
import com.tup.reconac.feature.auditoria.dtos.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaUpdateService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AuditoriaUpdateService implements IAuditoriaUpdateService {

    private final AuditoriaRepository repo;

    @Override
    @Transactional
    public AuditoriaResponse update(AuditoriaRequestDto req, UUID id) {

        Auditoria audit = repo.findById(id)
                .orElseThrow(() ->
                        new AuditoriaNotFoundException("Auditoria no encontrada"));

        AuditoriaMapper.updateEntity(audit, req);   // dirty checking
        return AuditoriaMapper.toResponse(audit);
    }
}
