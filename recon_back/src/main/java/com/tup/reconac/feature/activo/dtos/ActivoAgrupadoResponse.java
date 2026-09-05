package com.tup.reconac.feature.activo.dtos;

import java.util.List;
import java.util.UUID;

public record ActivoAgrupadoResponse(
        String host,
        String hostname,
        String so,
        Integer soProbab,
        String mac,
        String descripcion,
        List<UUID> escaneoIds
) {}
