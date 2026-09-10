package com.tup.reconac.modules.vulnEnum.dtos;

import java.util.UUID;

public record CpeResponse(
        UUID cpeId,
        String uri,
        String uriLegible,
        String servicioNombre,
        String vendor,
        String producto,
        String version
) {}
