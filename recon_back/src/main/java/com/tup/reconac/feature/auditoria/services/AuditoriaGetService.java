package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaEstadisticasResponse;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.mappers.AuditoriaMapper;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaGetService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.usuario.services.domain.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditoriaGetService implements IAuditoriaGetService {

    private final AuditoriaRepository repo;
    private final CurrentUserService currentUser;
    private final AuditoriaConsultService auditoriaConsult;
    private final EscaneoConsultService escaneoConsult;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> getAll(Pageable pageable) {
        UUID usuarioId = currentUser.getUsuarioId();

        return repo.findByUsuarioIdOrderByFechaGeneracionDesc(usuarioId, pageable)
                .map(AuditoriaMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriaResponse getById(UUID auditoriaId) {

        UUID usuarioId = currentUser.getUsuarioId();
        Auditoria auditoria = auditoriaConsult.findOwnedAuditoria(auditoriaId, usuarioId);

        return AuditoriaMapper.toResponse(auditoria);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriaEstadisticasResponse getEstadisticas(UUID auditoriaId) {

        auditoriaConsult.verifyAuditoriaOwnership(auditoriaId);
        List<Escaneo> escaneos = escaneoConsult.getEscaneos(auditoriaId);

        int completados = 0;
        int enProceso = 0;
        int pendientes = 0;
        int fallidos = 0;

        for (Escaneo escaneo : escaneos) {
            switch (escaneo.getEstado()) {
                case COMPLETADO -> completados++;
                case EN_PROCESO -> enProceso++;
                case PENDIENTE -> pendientes++;
                case FALLO -> fallidos++;
            }
        }

        return new AuditoriaEstadisticasResponse(
                escaneos.size(),
                completados,
                enProceso,
                pendientes,
                fallidos
        );
    }
}
