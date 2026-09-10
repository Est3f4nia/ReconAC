package com.tup.reconac.feature.auditoria.mappers;

import com.tup.reconac.feature.auditoria.dtos.request.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.usuario.models.Usuario;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaMapper {

    public static Auditoria toEntity(AuditoriaRequestDto req, Usuario usuario) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuarioId(usuario.getId());
        auditoria.setNombre(req.nombre());
        auditoria.setObjetivo(req.objetivo());
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
                auditoria.getFechaFinal()
        );
    }
}
