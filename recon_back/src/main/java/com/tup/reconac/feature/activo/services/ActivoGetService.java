package com.tup.reconac.feature.activo.services;

import com.tup.reconac.feature.activo.dtos.ActivoResponse;
import com.tup.reconac.feature.activo.mappers.ActivoMapper;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.activo.services.interfaces.IActivoGetService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ActivoGetService implements IActivoGetService {

    private final ActivoRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Page<ActivoResponse> getAll(Pageable pageable) {
        return repo.findAll(pageable).map(ActivoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivoResponse> getByEscaneoId(UUID escaneoId, Pageable pageable) {
        return repo.findByEscaneoId(escaneoId, pageable).map(ActivoMapper::toResponse);
    }
}
