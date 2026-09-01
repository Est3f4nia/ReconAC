package com.tup.reconac.feature.activo.mappers;

import com.tup.reconac.feature.activo.dtos.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.ActivoResponse;
import com.tup.reconac.feature.activo.models.Activo;
import org.springframework.stereotype.Component;

@Component
public class ActivoMapper {

    public static Activo toEntity(ActivoRequestDto req) {
        Activo activo = new Activo();
        activo.setEscaneoId(req.escaneoId());
        activo.setHost(req.host());
        activo.setHostname(req.hostname());
        activo.setSo(req.so());
        activo.setSoProbab(req.soProbab());
        activo.setMac(req.mac());
        activo.setDescripcion(req.descripcion());
        return activo;
    }

    public static void updateEntity(Activo activo, ActivoRequestDto req) {
        activo.setHost(req.host());
        activo.setHostname(req.hostname());
        activo.setSo(req.so());
        activo.setSoProbab(req.soProbab());
        activo.setMac(req.mac());
        activo.setDescripcion(req.descripcion());
    }

    public static ActivoResponse toResponse(Activo activo) {
        return new ActivoResponse(
                activo.getId(),
                activo.getHost(),
                activo.getHostname(),
                activo.getSo(),
                activo.getSoProbab(),
                activo.getMac(),
                activo.getDescripcion()
        );
    }
}
