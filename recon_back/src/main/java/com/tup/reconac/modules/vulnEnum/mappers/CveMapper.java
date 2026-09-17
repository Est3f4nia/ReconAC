package com.tup.reconac.modules.vulnEnum.mappers;

import com.tup.reconac.modules.vulnEnum.dtos.CveDetalleResponse;
import com.tup.reconac.modules.vulnEnum.models.Cve;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CveMapper {

    public static CveDetalleResponse toResponse(Cve cve) {
        return new CveDetalleResponse(
                cve.getCve(),
                cve.getDescripcion(),
                cve.getSeveridad(),
                cve.getCvss(),
                cve.getVectorCvss(),
                cve.getEpss(),
                Boolean.TRUE.equals(cve.getKev()),
                cve.getVersionVuln(),
                cve.getMitigacion(),
                cve.getVersionParche(),
                cve.getTipoParche(),
                cve.getExploitRefs() != null
                        ? Collections.singletonList(cve.getExploitRefs())
                        : List.of(),
                cve.getUrlNist(),
                cve.getFechaPublicacion(),
                cve.getUltModificacion()
        );
    }
}
