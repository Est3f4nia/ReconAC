package com.tup.reconac.feature.puerto.mapper;

import com.tup.reconac.feature.puerto.dtos.PuertoResultadoResponse;
import com.tup.reconac.feature.puerto.models.Puerto;

import java.util.List;

public class PuertoMapper {
    public static PuertoResultadoResponse toResultadoResponse(
            Puerto puerto,
            List<String> cpes) {

        return new PuertoResultadoResponse(
                puerto.getNumero(),
                puerto.getProtocolo(),
                puerto.getEstado(),
                puerto.getServicioFallback(),
                puerto.getProducto(),
                puerto.getVersion(),
                puerto.getExtraInfo(),
                cpes
        );
    }
}
