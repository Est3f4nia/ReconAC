package com.tup.reconac.feature.activo.dtos.response;

import java.util.UUID;

public record ActivoResponse(
        UUID id,
        UUID escaneoId,
        String host,
        String hostname,
        String so,
        Integer soProbab,
        String mac,
        String descripcion
) {}
