package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.exceptions.auditoria.AuditoriaNotFoundException;
import com.tup.reconac.feature.auditoria.dtos.request.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
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

    private final AuditoriaRepository repo;
    private final UserDetailsService userService;

    @Override
    @Transactional
    public AuditoriaResponse update(AuditoriaRequestDto req, UUID auditoriaId) {

        Usuario usuario = userService.getAuthenticatedUser();

        Auditoria audit = repo.findByIdAndUsuarioId(auditoriaId, usuario.getId())
                .orElseThrow(() ->
                        new AuditoriaNotFoundException("Auditoria no encontrada"));

        AuditoriaMapper.updateEntity(audit, req);   // dirty checking JPA
        return AuditoriaMapper.toResponse(audit);
    }
}
