package com.tup.reconac.feature.escaneo.services;

import tools.jackson.databind.ObjectMapper;
import com.tup.reconac.exceptions.global.BadRequestException;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.exceptions.auditoria.AuditoriaNotFoundException;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.ScanStartRequest;
import com.tup.reconac.feature.escaneo.dtos.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.mappers.EscaneoMapper;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScanService {

    private final EscaneoRepository escaneoRepo;
    private final AuditoriaRepository auditoriaRepo;
    private final UserDetailsService userService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.modules.api-url}")
    private String modulesApiUrl;

    @Transactional
    public EscaneoResponse startScan(UUID auditoriaId, ScanStartRequest req) {
        Auditoria auditoria = auditoriaRepo.findById(auditoriaId)
                .orElseThrow(() -> new AuditoriaNotFoundException("Auditoría no encontrada: " + auditoriaId));

        Usuario usuario = userService.getAuthenticatedUser();

        if (!auditoria.getUsuarioId().equals(usuario.getId())) {
            throw new BadRequestException("La auditoría no pertenece al usuario autenticado");
        }

        Escaneo escaneo = new Escaneo();
        escaneo.setAuditoriaId(auditoriaId);
        escaneo.setObjetivos(req.objetivos().toArray(new String[0]));
        escaneo.setEstado("QUEUED");

        escaneo = escaneoRepo.save(escaneo);

        try {
            Map<String, Object> body = Map.of(
                    "targets", req.objetivos(),
                    "timeout", req.timeout() != null ? req.timeout() : 600,
                    "icmp_timeout", req.icmpTimeout() != null ? req.icmpTimeout() : 5,
                    "max_cve_years", req.maxCveYears() != null ? req.maxCveYears() : 2,
                    "min_cvss_score", req.minCvssScore() != null ? req.minCvssScore() : 0.0
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    modulesApiUrl + "/scan", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String externalScanId = (String) response.getBody().get("scan_id");
                escaneo.setModuloJobId(externalScanId);
                escaneo.setEstado("RUNNING");
                escaneo.setIniciadoA(LocalDateTime.now());
                escaneoRepo.save(escaneo);
            } else {
                escaneo.setEstado("FAILED");
                escaneo.setMensajeError("No se pudo iniciar el escaneo en el módulo");
                escaneoRepo.save(escaneo);
            }
        } catch (Exception e) {
            escaneo.setEstado("FAILED");
            escaneo.setMensajeError("Error al conectar con el módulo: " + e.getMessage());
            escaneoRepo.save(escaneo);
        }

        return EscaneoMapper.toResponse(escaneo);
    }

    @Transactional(readOnly = true)
    public EscaneoResponse getStatus(UUID escaneoId) {
        Escaneo escaneo = escaneoRepo.findById(escaneoId)
                .orElseThrow(() -> new BadRequestException("Escaneo no encontrado: " + escaneoId));
        return EscaneoMapper.toResponse(escaneo);
    }

    @Transactional
    public void updateStatusFromExternal(UUID escaneoId, ScanStatusResponse externalStatus) {
        Escaneo escaneo = escaneoRepo.findById(escaneoId)
                .orElseThrow(() -> new BadRequestException("Escaneo no encontrado: " + escaneoId));

        escaneo.setEstado(externalStatus.status());

        if ("COMPLETED".equals(externalStatus.status())) {
            escaneo.setCompletadoA(LocalDateTime.now());
            escaneo.setProgreso(100);
            fetchAndStoreResult(escaneo, externalStatus.scanId());
        } else if ("FAILED".equals(externalStatus.status())) {
            escaneo.setCompletadoA(LocalDateTime.now());
            escaneo.setMensajeError(externalStatus.error());
        }

        escaneoRepo.save(escaneo);
    }

    private void fetchAndStoreResult(Escaneo escaneo, String externalScanId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    modulesApiUrl + "/result/" + externalScanId, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                escaneo.setResultado(objectMapper.writeValueAsString(response.getBody()));
            }
        } catch (Exception e) {
            escaneo.setMensajeError("Escaneo completado pero no se pudo obtener resultado: " + e.getMessage());
        }
    }
}
