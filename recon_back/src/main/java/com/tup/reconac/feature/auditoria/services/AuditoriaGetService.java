package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.exceptions.auditoria.AuditoriaNotFoundException;
import com.tup.reconac.feature.auditoria.dtos.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaGetService;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AuditoriaGetService implements IAuditoriaGetService {

    private final AuditoriaRepository repo;
    private final UserDetailsService userService;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> getAll(Pageable pageable) {
        UUID usuarioId = userService
                .getAuthenticatedUser()
                .getId();

        return repo
                .findByUsuarioIdOrderByFechaGeneracionDesc(
                        usuarioId,
                        pageable
                )
                .map(AuditoriaMapper::toResponse);
    }

    // devolver auditoria + escaneo
    @Override
    @Transactional(readOnly = true)
    public AuditoriaResponse getById(UUID id) {

        UUID usuarioId = userService.getAuthenticatedUser().getId();

        Auditoria auditoria = repo
                .findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() ->
                        new AuditoriaNotFoundException("Auditoría no encontrada")
                );

        return AuditoriaMapper.toResponse(auditoria);
    }
}
