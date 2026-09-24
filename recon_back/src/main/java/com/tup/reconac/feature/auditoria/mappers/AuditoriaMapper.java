package com.tup.reconac.feature.auditoria.mappers;

import com.tup.reconac.feature.auditoria.dtos.request.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResumenResponseDto;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuditoriaMapper {

    public static Auditoria toEntity(AuditoriaRequestDto req, UUID usuarioId) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuarioId(usuarioId);
        auditoria.setNombre(req.nombre() != null ? req.nombre().strip() : null);
        auditoria.setObjetivo(req.objetivo());
        return auditoria;
    }

    public static void updateEntity(Auditoria auditoria, AuditoriaRequestDto req) {
        if (req.nombre() != null) auditoria.setNombre(req.nombre().strip());
        if (req.objetivo() != null) auditoria.setObjetivo(req.objetivo().strip());
    }

    public static AuditoriaResponse toResponse(Auditoria auditoria) {
        return new AuditoriaResponse(
                auditoria.getId(),
                auditoria.getNombre(),
                auditoria.getObjetivo(),
                auditoria.getFechaGeneracion(),
                auditoria.getFechaFinal()
        );
    }

    public static AuditoriaResumenResponseDto toResumenResponse(Auditoria auditoria) {
        return new AuditoriaResumenResponseDto(
                null,
                auditoria.getId(),
                auditoria.getNombre(),
                0,
                0,
                null,
                null,
                0,
                0
        );
    }

    public static AuditoriaResumenResponseDto toResumenResponse(
            Auditoria auditoria,
            Escaneo escaneo,
            int activos,
            int puertos,
            int cves,
            int cvesCriticos) {

        return new AuditoriaResumenResponseDto(
                escaneo.getId(),
                auditoria.getId(),
                auditoria.getNombre(),
                activos,
                puertos,
                escaneo.getCompletadoA(),
                escaneo.getEstado(),
                cves,
                cvesCriticos
        );
    }
}
