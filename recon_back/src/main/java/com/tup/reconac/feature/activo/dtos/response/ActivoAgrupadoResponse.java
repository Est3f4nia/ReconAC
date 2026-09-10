package com.tup.reconac.feature.activo.dtos.response;

import java.util.List;
import java.util.UUID;

// Devuelve activos deduplicados
public record ActivoAgrupadoResponse(
        String host,
        String hostname,
        String so,
        Integer soProbab,
        String mac,
        String descripcion,
        List<UUID> escaneoIds
) {}
