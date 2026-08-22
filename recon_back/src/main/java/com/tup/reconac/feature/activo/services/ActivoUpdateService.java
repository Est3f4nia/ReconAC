package com.tup.reconac.feature.activo.services;

import com.tup.reconac.exceptions.activo.ActivoNotFoundException;
import com.tup.reconac.feature.activo.dtos.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.ActivoResponse;
import com.tup.reconac.feature.activo.mappers.ActivoMapper;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.activo.services.interfaces.IActivoUpdateService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ActivoUpdateService implements IActivoUpdateService {

    private final ActivoRepository repo;

    @Override
    @Transactional
    public ActivoResponse update(ActivoRequestDto req, UUID id) {
        Activo activo = repo.findById(id)
                .orElseThrow(() -> new ActivoNotFoundException("Activo no encontrado"));

        ActivoMapper.updateEntity(activo, req);
        return ActivoMapper.toResponse(activo);
    }
}
