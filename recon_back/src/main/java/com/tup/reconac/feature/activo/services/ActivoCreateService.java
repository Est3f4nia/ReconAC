package com.tup.reconac.feature.activo.services;

import com.tup.reconac.feature.activo.dtos.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.ActivoResponse;
import com.tup.reconac.feature.activo.mappers.ActivoMapper;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.activo.services.interfaces.IActivoCreateService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ActivoCreateService implements IActivoCreateService {

    private final ActivoRepository repo;

    @Override
    @Transactional
    public ActivoResponse create(ActivoRequestDto req) {
        Activo activo = ActivoMapper.toEntity(req);
        Activo saved = repo.save(activo);
        return ActivoMapper.toResponse(saved);
    }
}
