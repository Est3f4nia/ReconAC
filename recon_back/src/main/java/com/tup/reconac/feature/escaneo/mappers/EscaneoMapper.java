package com.tup.reconac.feature.escaneo.mappers;

import com.tup.reconac.feature.escaneo.dtos.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.ScanStatusResponse;
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

    public static ScanStatusResponse toStatusResponse(Escaneo escaneo) {
        return new ScanStatusResponse(
                escaneo.getModuloJobId(),
                escaneo.getEstado(),
                escaneo.getProgreso(),
                escaneo.getMensajeError()
        );
    }
}
