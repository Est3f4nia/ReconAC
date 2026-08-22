package com.tup.reconac.feature.activo.services;

import com.tup.reconac.feature.activo.dtos.ActivoResponse;
import com.tup.reconac.feature.activo.mappers.ActivoMapper;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.activo.services.interfaces.IActivoGetService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ActivoGetService implements IActivoGetService {

    private final ActivoRepository repo;

    @Override
    @Transactional(readOnly = true)
    public List<ActivoResponse> getAll() {
        return repo.findAll()
                .stream()
                .map(ActivoMapper::toResponse)
                .collect(Collectors.toList());
    }
}
