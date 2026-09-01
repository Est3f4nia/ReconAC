package com.tup.reconac.feature.activo.dtos;

import java.util.UUID;

public record ActivoResponse(
        UUID id,
        String host,
        String hostname,
        String so,
        Integer soProbab,
        String mac,
        String descripcion
) {}
