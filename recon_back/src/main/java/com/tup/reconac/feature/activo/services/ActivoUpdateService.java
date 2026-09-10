package com.tup.reconac.feature.activo.services;

import com.tup.reconac.exceptions.activo.ActivoNotFoundException;
import com.tup.reconac.feature.activo.dtos.request.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.response.ActivoResponse;
import com.tup.reconac.feature.activo.mappers.ActivoMapper;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.activo.services.interfaces.IActivoUpdateService;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ActivoUpdateService implements IActivoUpdateService {

    private final ActivoRepository repo;
    private final EscaneoConsultService escaneoConsult;
    private final UserDetailsService userService;

    @Override
    @Transactional
    public ActivoResponse update(ActivoRequestDto req, UUID id) {

        Usuario usuario = userService.getAuthenticatedUser();

        Activo activo = repo.findById(id)
                .orElseThrow(() ->
                        new ActivoNotFoundException(
                                "Activo no encontrado"
                        )
                );

        escaneoConsult.findEscaneoForUsuario(activo.getEscaneoId(), usuario.getId());

        ActivoMapper.updateEntity(activo, req);

        Activo saved = repo.save(activo);
        return ActivoMapper.toResponse(saved);
    }
}
