package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.auditoria.dtos.request.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaUpdateService;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AuditoriaUpdateService implements IAuditoriaUpdateService {

    private final UserDetailsService userService;
    private final AuditoriaConsultService auditoriaConsult;

    @Override
    @Transactional
    public AuditoriaResponse update(AuditoriaRequestDto req, UUID auditoriaId) {

        Usuario usuario = userService.getAuthenticatedUser();
        Auditoria audit = auditoriaConsult.findOwnedAuditoria(auditoriaId, usuario.getId());

        if (audit.getNombre() == null) audit.setNombre("Nueva auditoria");

        AuditoriaMapper.updateEntity(audit, req);  // dirty checking JPA
        return AuditoriaMapper.toResponse(audit);
    }
}
