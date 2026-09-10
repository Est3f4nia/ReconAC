package com.tup.reconac.feature.escaneo.mappers;

import com.tup.reconac.feature.escaneo.dtos.response.EscaneoListadoResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class EscaneoMapper {

    public static EscaneoResponse toResponse(Escaneo escaneo) {
        return new EscaneoResponse(
                escaneo.getId(),
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
                escaneo.getId(),   // hdppalgjvaknsdfna'lskdnvaonv dkl
                escaneo.getEstado(),
                escaneo.getProgreso(),
                escaneo.getMensajeError()
        );
    }

    // corregir uso
    public static EscaneoListadoResponse toResponseListado (Escaneo escaneo, Map<UUID, String> nombresAuditorias) {
        return new EscaneoListadoResponse(
                escaneo.getId(),
                escaneo.getAuditoriaId(),
                nombresAuditorias.get(escaneo.getAuditoriaId()),
                List.of(escaneo.getObjetivos()),
                escaneo.getEstado(),
                escaneo.getProgreso(),
                escaneo.getNmapVersion(),
                escaneo.getIniciadoA(),
                escaneo.getCompletadoA()
        );
    }

}
