package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.exceptions.auditoria.AuditoriaNotFoundException;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaEstadisticasResponse;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaGetService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuditoriaGetService implements IAuditoriaGetService {

    private final AuditoriaRepository repo;
    private final UserDetailsService userService;
    private final EscaneoConsultService escaneoConsult;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> getAll(Pageable pageable) {
        UUID usuarioId = userService.getAuthenticatedUser().getId();

        return repo.findByUsuarioIdOrderByFechaGeneracionDesc(usuarioId, pageable)
                .map(AuditoriaMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriaResponse getById(UUID id) {

        UUID usuarioId = userService.getAuthenticatedUser().getId();

        Auditoria auditoria = repo.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() ->
                        new AuditoriaNotFoundException("Auditoría no encontrada")
                );

        return AuditoriaMapper.toResponse(auditoria);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriaEstadisticasResponse getEstadisticas(UUID autitoriaId) {

        UUID usuarioId = userService.getAuthenticatedUser().getId();

        repo.findByIdAndUsuarioId(autitoriaId, usuarioId)
                .orElseThrow(() ->
                        new AuditoriaNotFoundException("Auditoría no encontrada")
                );

        List<Escaneo> escaneos = escaneoConsult.getEscaneos(autitoriaId);

        int total = escaneos.size();

        int completados = (int) escaneos.stream()
                .filter(e -> e.getEstado() == EscaneoEstado.COMPLETADO)
                .count();

        int enProceso = (int) escaneos.stream()
                .filter(e -> e.getEstado() == EscaneoEstado.EN_PROCESO)
                .count();

        int pendientes = (int) escaneos.stream()
                .filter(e -> e.getEstado() == EscaneoEstado.PENDIENTE)
                .count();

        int fallidos = (int) escaneos.stream()
                .filter(e -> e.getEstado() == EscaneoEstado.FALLO)
                .count();

        return new AuditoriaEstadisticasResponse(
                total,
                completados,
                enProceso,
                pendientes,
                fallidos
        );
    }
}
