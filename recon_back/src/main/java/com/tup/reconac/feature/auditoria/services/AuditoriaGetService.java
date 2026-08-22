package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.auditoria.dtos.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaGetService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AuditoriaGetService implements IAuditoriaGetService {

    private final AuditoriaRepository auditoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaResponse>  getAll() {
        return auditoriaRepository.findAll()
                .stream()
                .map(AuditoriaMapper::toResponse)
                .collect(Collectors.toList());
    }


}

/**
 * @Override
 *     @Transactional(readOnly = true)
 *     public List<PronosticoResponseDto> listarMisPronosticos() {
 *
 *         Usuario usuario = validateUser.getAuthenticatedUserSession();
 *
 *         return pronosticoRepository.findByUsuarioId(usuario.getId())
 *                 .stream()
 *                 .map(pronosticoMapper::toDto)
 *                 .toList();
 *     }
 */