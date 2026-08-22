package com.tup.reconac.feature.activo.services;

import com.tup.reconac.exceptions.activo.ActivoNotFoundException;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.activo.services.interfaces.IActivoDeleteService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ActivoDeleteService implements IActivoDeleteService {

    private final ActivoRepository repo;

    @Override
    @Transactional
    public void deleteById(UUID id) {
        if (!repo.existsById(id))
            throw new ActivoNotFoundException("El activo no existe");

        repo.deleteById(id);
    }
}
