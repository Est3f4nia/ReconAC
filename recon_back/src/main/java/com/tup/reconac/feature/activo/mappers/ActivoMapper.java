package com.tup.reconac.feature.activo.mappers;

import com.tup.reconac.feature.activo.dtos.response.ActivoAgrupadoResponse;
import com.tup.reconac.feature.activo.dtos.request.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.response.ActivoResponse;
import com.tup.reconac.feature.activo.models.Activo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

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
                activo.getEscaneoId(),
                activo.getHost(),
                activo.getHostname(),
                activo.getSo(),
                activo.getSoProbab(),
                activo.getMac(),
                activo.getDescripcion()
        );
    }

    public static ActivoAgrupadoResponse toAgrupadoResponse(List<Activo> activos) {

        Activo activo = activos.getFirst();

        List<UUID> escaneoIds = activos.stream()
                .map(Activo::getEscaneoId)
                .distinct()
                .toList();

        return new ActivoAgrupadoResponse(
                activo.getHost(),
                activo.getHostname(),
                activo.getSo(),
                activo.getSoProbab(),
                activo.getMac(),
                activo.getDescripcion(),
                escaneoIds
        );
    }
}
