package com.tup.reconac.feature.escaneo.services.interfaces;

import com.tup.reconac.feature.auditoria.models.AuditoriaReporte;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;

import java.util.UUID;

public interface IEscaneoReportService {
    ResponseEntity<Resource> generar(
            UUID auditoriaId,
            UUID escaneoId,
            AuditoriaReporte formato
    );
}
