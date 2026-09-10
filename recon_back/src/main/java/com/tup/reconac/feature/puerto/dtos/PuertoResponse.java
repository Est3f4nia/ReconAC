package com.tup.reconac.feature.puerto.dtos;

import com.tup.reconac.modules.vulnEnum.dtos.CpeResponse;

import java.util.List;
import java.util.UUID;

public record PuertoResponse(
        UUID puertoId,
        Integer numero,
        String protocolo,
        String estado,
        String servicio,
        List<CpeResponse> cpes     // hook con vulnEnum
) {}
