package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.config.modules.ModulesConfig;
import com.tup.reconac.exceptions.global.BadRequestException;
import com.tup.reconac.feature.auditoria.models.AuditoriaReporte;
import com.tup.reconac.feature.escaneo.dtos.request.ModuleReportRequest;
import com.tup.reconac.feature.escaneo.mappers.EscaneoModulesMapper;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoReportService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscaneoReportService implements IEscaneoReportService {

    private final EscaneoConsultService escaneoConsult;
    private final ModulesConfig modulesConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public ResponseEntity<Resource> generar(
            UUID auditoriaId,
            UUID escaneoId,
            AuditoriaReporte formato) {

        Escaneo escaneo = escaneoConsult.findEscaneoForAuditoria(auditoriaId, escaneoId);

        if (escaneo.getEstado() != EscaneoEstado.COMPLETADO) {
            throw new BadRequestException(
                    "Solo se pueden generar reportes de escaneos completados"
            );
        }

        if (escaneo.getResultado() == null || escaneo.getResultado().isBlank()) {
            throw new BadRequestException(
                    "El escaneo no tiene un resultado disponible"
            );
        }

        log.info(
                "Generando reporte {} para escaneo {} de auditoría {}",
                formato,
                escaneoId,
                auditoriaId
        );

        return solicitarReporte(escaneo, formato);
    }

    private ResponseEntity<Resource> solicitarReporte(
            Escaneo escaneo,
            AuditoriaReporte formato) {

        try {
            String moduleUrl = modulesConfig.getEndpoint("recon").getUrl();

            JsonNode resultado = objectMapper.readTree(escaneo.getResultado());

            ModuleReportRequest body = EscaneoModulesMapper.toReportRequest(
                    escaneo,
                    formato,
                    resultado
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ModuleReportRequest> request = new HttpEntity<>(body, headers);

            log.info(
                    "Solicitando reporte al módulo de reconocimiento. escaneoId={}, formato={}",
                    escaneo.getId(),
                    formato
            );

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    moduleUrl + "/report",
                    HttpMethod.POST,
                    request,
                    byte[].class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException(
                        "El módulo de reconocimiento no pudo generar el reporte"
                );
            }

            byte[] content = response.getBody();

            if (content == null || content.length == 0) {
                throw new IllegalStateException(
                        "El módulo devolvió un reporte vacío"
                );
            }

            MediaType contentType = response.getHeaders().getContentType();
            String filename = obtenerFilename(response, escaneo.getId(), formato);

            log.info(
                    "Reporte generado correctamente. escaneoId={}, filename={}, bytes={}",
                    escaneo.getId(),
                    filename,
                    content.length
            );

            ByteArrayResource resource = new ByteArrayResource(content);

            return ResponseEntity.ok()
                    .contentType(contentType != null
                            ? contentType
                            : MediaType.APPLICATION_OCTET_STREAM)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(filename)
                                    .build()
                                    .toString()
                    )
                    .contentLength(content.length)
                    .body(resource);

        } catch (HttpStatusCodeException e) {

            log.error(
                    "Error HTTP al generar reporte para escaneo {}: {}",
                    escaneo.getId(),
                    e.getStatusCode(),
                    e
            );

            throw new IllegalStateException(
                    "Error al generar el reporte: " + e.getResponseBodyAsString(),
                    e
            );

        } catch (Exception e) {

            log.error(
                    "No se pudo generar el reporte para escaneo {}",
                    escaneo.getId(),
                    e
            );

            throw new IllegalStateException(
                    "No se pudo generar el reporte",
                    e
            );
        }
    }

    private String obtenerFilename(
            ResponseEntity<byte[]> response,
            UUID escaneoId,
            AuditoriaReporte formato) {

        String contentDisposition = response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION);

        if (contentDisposition != null) {
            try {
                String filename = ContentDisposition.parse(contentDisposition).getFilename();

                if (filename != null && !filename.isBlank()) {
                    return filename;
                }
            } catch (IllegalArgumentException e) {
                log.warn(
                        "Content-Disposition inválido recibido del módulo para escaneo {}: {}",
                        escaneoId,
                        contentDisposition
                );
            }
        }

        String extension = switch (formato) {
            case MD -> ".md";
            case CSV -> ".zip";
        };

        return "reconac_" + escaneoId + extension;
    }
}
