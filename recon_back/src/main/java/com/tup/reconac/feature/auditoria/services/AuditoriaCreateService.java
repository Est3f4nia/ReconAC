package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.auditoria.dtos.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaCreateService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuditoriaCreateService implements IAuditoriaCreateService {

    private final AuditoriaRepository repo;

    @Override
    @Transactional
    public AuditoriaResponse create(AuditoriaRequestDto req) {
        // Usuario usuario = validateUser.getAuthenticatedUserSession();

        Auditoria audit = AuditoriaMapper.toEntity(req);

        if (audit.getNombre().isEmpty())
            audit.setNombre("Nueva Auditoría");

        Auditoria saved = repo.save(audit);
        return AuditoriaMapper.toResponse(saved);
    }
}