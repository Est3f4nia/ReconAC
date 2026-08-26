package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.auditoria.dtos.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaGetService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuditoriaGetService implements IAuditoriaGetService {

    private final AuditoriaRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> getAll(Pageable pageable) {
        return repo.findAll(pageable).map(AuditoriaMapper::toResponse);
    }
}
