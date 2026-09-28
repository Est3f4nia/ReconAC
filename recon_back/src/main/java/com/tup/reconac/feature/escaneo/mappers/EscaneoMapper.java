package com.tup.reconac.feature.escaneo.mappers;

import com.tup.reconac.feature.activo.dtos.response.ActivoResultadoResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoListadoResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResult;
import com.tup.reconac.feature.escaneo.models.Escaneo;

import java.util.List;

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
                escaneo.getId(),
                escaneo.getEstado(),
                escaneo.getProgreso(),
                escaneo.getMensajeError()
        );
    }

    public static EscaneoListadoResponse toListadoResponse(Escaneo escaneo, String auditoriaNombre) {
        return new EscaneoListadoResponse(
                escaneo.getId(),
                escaneo.getAuditoriaId(),
                auditoriaNombre,
                List.of(escaneo.getObjetivos()),
                escaneo.getEstado(),
                escaneo.getProgreso(),
                escaneo.getNmapVersion(),
                escaneo.getIniciadoA(),
                escaneo.getCompletadoA()
        );
    }

    public static EscaneoResult.EscaneoResultResponse toResultResponse(
            Escaneo escaneo,
            List<ActivoResultadoResponse> activos) {

        return new EscaneoResult.EscaneoResultResponse(
                escaneo.getId(),
                escaneo.getEstado(),
                escaneo.getProgreso(),
                escaneo.getNmapVersion(),
                escaneo.getIniciadoA(),
                escaneo.getCompletadoA(),
                activos
        );
    }
}
