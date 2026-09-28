package com.tup.reconac.feature.puerto.dtos;

import com.tup.reconac.feature.puerto.models.PuertoEstado;

import java.util.List;

public record PuertoResultadoResponse(
        Integer numero,
        String protocolo,
        PuertoEstado estado,
        String servicio,
        String producto,
        String version,
        String extraInfo,
        List<String> cpes
) {}