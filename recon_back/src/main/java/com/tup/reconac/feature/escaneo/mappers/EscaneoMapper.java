package com.tup.reconac.feature.escaneo.mappers;

import com.tup.reconac.feature.escaneo.dtos.EscaneoResponse;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import org.springframework.stereotype.Component;

@Component
public class EscaneoMapper {

    public static EscaneoResponse toResponse(Escaneo escaneo) {
        return new EscaneoResponse(
                escaneo.getId(),
                escaneo.getAuditoriaId(),
                escaneo.getObjetivos(),
                escaneo.getEstado(),
                escaneo.getProgreso(),
                escaneo.getNmapVersion(),
                escaneo.getMensajeError(),
                escaneo.getIniciadoA(),
                escaneo.getCompletadoA(),
                escaneo.getCreadoA()
        );
    }
}
