package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.auditoria.dtos.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaCreateService;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuditoriaCreateService implements IAuditoriaCreateService {

    private final AuditoriaRepository repo;
    private final UserDetailsService userService;

    @Override
    @Transactional
    public AuditoriaResponse create(AuditoriaRequestDto req) {
        Usuario usuario = userService.getAuthenticatedUser();

        Auditoria audit = AuditoriaMapper.toEntity(req, usuario);

        if (audit.getNombre() == null || audit.getNombre().isBlank())
            audit.setNombre("Nueva Auditoría");

        Auditoria saved = repo.save(audit);
        return AuditoriaMapper.toResponse(saved);
    }
}