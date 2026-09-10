package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.exceptions.auditoria.AuditoriaNotFoundException;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaDeleteService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AuditoriaDeleteService implements IAuditoriaDeleteService {

    private final AuditoriaRepository repo;

    @Override
    @Transactional
    public void deleteById(UUID id) {

        if (!repo.existsById(id))
            throw new AuditoriaNotFoundException("La auditoria no existe");

        repo.deleteById(id);

        // Arquitectura: por el tamaño potencial de `auditoría`, no aplica usar soft deleteEscaneo
    }
}
