package com.tup.reconac.feature.auditoria.mappers;

import com.tup.reconac.feature.auditoria.dtos.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditoriaMapper {

    public static Auditoria toEntity(AuditoriaRequestDto req) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuarioId(req.usuarioId());
        auditoria.setNombre(req.nombre());
        auditoria.setObjetivo(req.objetivo());
        auditoria.setFechaGeneracion(LocalDateTime.now());
        return auditoria;
    }

    public static void updateEntity(Auditoria auditoria, AuditoriaRequestDto req) {
        auditoria.setNombre(req.nombre());
        auditoria.setObjetivo(req.objetivo());
    }

    public static AuditoriaResponse toResponse(Auditoria auditoria) {
        return new AuditoriaResponse(
                auditoria.getId(),
                auditoria.getNombre(),
                auditoria.getObjetivo(),
                auditoria.getFechaGeneracion(),
                auditoria.getFechaFinal(),
                auditoria.getNmapVersion()
        );
    }
}
