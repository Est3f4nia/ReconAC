package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.auditoria.dtos.request.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaUpdateService;
import com.tup.reconac.feature.usuario.services.domain.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditoriaUpdateService implements IAuditoriaUpdateService {

    private final CurrentUserService currentUser;
    private final AuditoriaConsultService auditoriaConsult;

    @Override
    @Transactional
    public AuditoriaResponse update(AuditoriaRequestDto req, UUID auditoriaId) {

        UUID usuarioId = currentUser.getUsuarioId();
        Auditoria auditoria = auditoriaConsult.findOwnedAuditoria(auditoriaId, usuarioId);

        AuditoriaMapper.updateEntity(auditoria, req);

        if (auditoria.getNombre() == null || auditoria.getNombre().isBlank()) {
            auditoria.setNombre("Nueva auditoria");
        }

        return AuditoriaMapper.toResponse(auditoria);
    }
}
