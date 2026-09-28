package com.tup.reconac.feature.escaneo.mappers;

import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.escaneo.dtos.response.HostResult;
import com.tup.reconac.feature.puerto.dtos.PuertoResultadoResponse;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.modules.vulnEnum.mappers.CpeParser;
import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.models.PuertoCpe;

import java.util.UUID;

public final class EscaneoResultMapper {

    public static Activo toActivo(UUID escaneoId, HostResult host) {

        Activo activo = new Activo();
        activo.setEscaneoId(escaneoId);
        activo.setHost(host.ip());
        activo.setMac(host.mac());
        activo.setHostname(host.hostname());
        activo.setSo(host.os());
        activo.setSoProbab(host.soProbab());

        return activo;
    }

    public static Puerto toPuerto(UUID activoId, PuertoResultadoResponse result) {

        Puerto puerto = new Puerto();
        puerto.setActivoId(activoId);
        puerto.setNumero(result.numero());
        puerto.setProtocolo(result.protocolo());
        puerto.setEstado(result.estado());
        puerto.setServicioFallback(result.servicio());
        puerto.setProducto(result.producto());
        puerto.setVersion(result.version());
        puerto.setExtraInfo(result.extraInfo());

        return puerto;
    }

    public static Cpe toCpe(String uri) {

        Cpe cpe = new Cpe();
        cpe.setUri(uri);
        cpe.setUriLegible(uri);
        cpe.setUltimoCheck(null);

        CpeParser.enrich(cpe);
        return cpe;
    }

    public static PuertoCpe toPuertoCpe(UUID puertoId, UUID cpeId) {

        PuertoCpe puertoCpe = new PuertoCpe();
        puertoCpe.setPuertoId(puertoId);
        puertoCpe.setCpeId(cpeId);

        return puertoCpe;
    }
}
