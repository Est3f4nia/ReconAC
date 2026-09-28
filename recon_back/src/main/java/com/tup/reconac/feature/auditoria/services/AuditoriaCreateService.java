package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.auditoria.dtos.request.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaCreateService;
import com.tup.reconac.feature.usuario.services.domain.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditoriaCreateService implements IAuditoriaCreateService {

    private final AuditoriaRepository repo;
    private final CurrentUserService currentUser;

    @Override
    @Transactional
    public AuditoriaResponse create(AuditoriaRequestDto req) {

        UUID usuarioId = currentUser.getUsuarioId();
        Auditoria auditoria = AuditoriaMapper.toEntity(req, usuarioId);

        if (auditoria.getNombre() == null || auditoria.getNombre().isBlank())
            auditoria.setNombre("Nueva Auditoría");

        return AuditoriaMapper.toResponse(repo.save(auditoria));
    }
}