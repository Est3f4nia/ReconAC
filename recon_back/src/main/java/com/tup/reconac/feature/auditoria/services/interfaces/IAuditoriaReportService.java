package com.tup.reconac.feature.auditoria.services.interfaces;

import com.tup.reconac.feature.auditoria.models.AuditoriaReporte;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;

import java.util.UUID;

public interface  IAuditoriaReportService {
    ResponseEntity<Resource> generar(
            UUID auditoriaId,
            UUID escaneoId,
            AuditoriaReporte formato
    );
}
