package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.config.ModulesConfig;
import com.tup.reconac.exceptions.auditoria.AuditoriaNotFoundException;
import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.exceptions.global.BadRequestException;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.models.AuditoriaReporte;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaReportService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuditoriaReportService implements IAuditoriaReportService {

    private final EscaneoRepository escaneoRepository;
    private final AuditoriaConsultService auditoriaConsult;
    private final UserDetailsService userService;
    private final ModulesConfig modulesConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> generar(UUID auditoriaId, UUID escaneoId, AuditoriaReporte formato) {

        Usuario usuario = userService.getAuthenticatedUser();
        Auditoria auditoria = auditoriaConsult.findId(auditoriaId);

        if (!auditoria.getUsuarioId().equals(usuario.getId())) {
            throw new AuditoriaNotFoundException("Auditoría no encontrada");
        }

        Escaneo escaneo = escaneoRepository.findById(escaneoId)
                .orElseThrow(() ->
                        new EscaneoNotFoundException("Escaneo no encontrado")
                );

        if (!escaneo.getAuditoriaId().equals(auditoriaId)) {
            throw new EscaneoNotFoundException("Escaneo no encontrado");
        }

        if (escaneo.getEstado() != EscaneoEstado.COMPLETADO) {
            throw new BadRequestException("Solo se pueden generar reportes de escaneos completados");
        }

        if (escaneo.getResultado() == null || escaneo.getResultado().isBlank()) {
            throw new BadRequestException("El escaneo no tiene un resultado disponible");
        }

        return solicitarReporte(escaneo, formato);
    }

    private ResponseEntity<Resource> solicitarReporte(Escaneo escaneo, AuditoriaReporte formato) {

        try {
            String moduleUrl = modulesConfig.getEndpoint("recon").getUrl();
            Map<String, Object> body = new LinkedHashMap<>();

            body.put(
                    "formato",
                    formato.name().toLowerCase()
            );

            body.put(
                    "scan_id",
                    escaneo.getId().toString()
            );

            body.put(
                    "resultado",
                    objectMapper.readValue(
                            escaneo.getResultado(),
                            Object.class
                    )
            );

            System.out.println("[Reporte] Resultado almacenado para escaneo:" + escaneo.getId());

            System.out.println(
                    "DEBUG REPORTE -> body enviado a Python:\n"
                            + objectMapper.writeValueAsString(body)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    moduleUrl + "/report",
                    HttpMethod.POST,
                    request,
                    byte[].class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("El módulo de reconocimiento no pudo generar el reporte");
            }

            byte[] content = response.getBody();

            if (content == null || content.length == 0) {
                throw new IllegalStateException("El módulo devolvió un reporte vacío");
            }

            MediaType contentType = response.getHeaders().getContentType();
            String filename = obtenerFilename(response, escaneo.getId(), formato);

            ByteArrayResource resource = new ByteArrayResource(content);

            System.out.println("[Reporte] Archivo generado: " + filename);

            return ResponseEntity.ok()
                    .contentType(
                            contentType != null
                                    ? contentType
                                    : MediaType.APPLICATION_OCTET_STREAM
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition
                                    .attachment()
                                    .filename(filename)
                                    .build()
                                    .toString()
                    )
                    .contentLength(content.length)
                    .body(resource);

        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException("Error al generar el reporte: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el reporte", e);
        }
    }

    private String obtenerFilename(ResponseEntity<byte[]> response, UUID escaneoId, AuditoriaReporte formato) {

        String contentDisposition = response.getHeaders()
                .getFirst(
                        HttpHeaders.CONTENT_DISPOSITION
                );

        if (contentDisposition != null) {
            try {
                ContentDisposition disposition = ContentDisposition.parse(contentDisposition);
                if (disposition.getFilename() != null) {
                    return disposition.getFilename();
                }
            } catch (Exception ignored) {
                // Se utiliza el nombre por defecto.
            }
        }

        return "reconac_"
                + escaneoId
                + (formato == AuditoriaReporte.MD
                ? ".md"
                : ".zip");
    }
}
