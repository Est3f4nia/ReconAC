package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.dtos.internal.ModuleScanRequest;
import com.tup.reconac.feature.escaneo.dtos.internal.ModuleScanResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStartRequest;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.mappers.EscaneoMapper;
import com.tup.reconac.feature.escaneo.mappers.EscaneoModulesMapper;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoCreateService;
import com.tup.reconac.config.modules.ModulesConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscaneoCreateService implements IEscaneoCreateService {

    private static final int MAX_MODULE_START_ATTEMPTS = 3;
    private static final long MODULE_RETRY_DELAY_MS = 500;

    private final EscaneoRepository repo;
    private final AuditoriaConsultService auditoriaConsult;
    private final ModulesConfig modulesConfig;
    private final RestTemplate restTemplate;
    private final EscaneoPersistenceService persistenceService;

    @Override
    public EscaneoResponse startScan(UUID auditoriaId, ScanStartRequest req) {

        auditoriaConsult.verifyAuditoriaOwnership(auditoriaId);

        String jobId = UUID.randomUUID().toString();

        Escaneo saved = persistenceService.crear(
                auditoriaId,
                req.objetivos().toArray(new String[0]),
                jobId
        );

        try {
            String moduleBaseUrl = modulesConfig.getEndpoint("recon").getUrl();

            ModuleScanRequest body = EscaneoModulesMapper.toModuleRequest(saved, req);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ModuleScanRequest> request = new HttpEntity<>(body, headers);
            ResponseEntity<ModuleScanResponse> response = iniciarModuloConRetry(
                    moduleBaseUrl,
                    request,
                    jobId
            );

            validarRespuestaModulo(response);

            saved.setEstado(EscaneoEstado.EN_PROCESO);
            saved.setIniciadoA(LocalDateTime.now());
            saved.setMensajeError(null);

            repo.save(saved);

        } catch (ResourceAccessException e) {
            saved.setEstado(EscaneoEstado.FALLO);
            saved.setMensajeError(
                    "No se pudo conectar con el módulo de reconocimiento después de "
                            + MAX_MODULE_START_ATTEMPTS + " intentos: " + e.getMessage()
            );

            repo.save(saved);

        } catch (Exception e) {
            saved.setEstado(EscaneoEstado.FALLO);
            saved.setMensajeError("Error al iniciar el escaneo: " + e.getMessage());

            repo.save(saved);
        }

        return EscaneoMapper.toResponse(saved);
    }

    @Override
    public void updateStatusFromExternal(String jobId, ScanStatusResponse status) {
        persistenceService.actualizarStatus(jobId, status);
    }

    @Override
    public void processCallback(String jobId, EscaneoResult result) {

        try {
            persistenceService.completarCallback(jobId, result);

        } catch (EscaneoNotFoundException e) {
            throw e;

        } catch (Exception e) {
            String mensaje = "Error al procesar el resultado del escaneo: " + e.getMessage();

            persistenceService.marcarFalloCallback(jobId, mensaje);

            throw new IllegalStateException(
                    "Error procesando callback del escaneo " + jobId,
                    e
            );
        }
    }

    private ResponseEntity<ModuleScanResponse> iniciarModuloConRetry(
            String moduleBaseUrl,
            HttpEntity<ModuleScanRequest> request,
            String jobId) {

        ResourceAccessException lastException = null;

        for (int attempt = 1; attempt <= MAX_MODULE_START_ATTEMPTS; attempt++) {
            try {
                log.info(
                        "Iniciando escaneo en módulo. Intento {}/{}, jobId={}",
                        attempt,
                        MAX_MODULE_START_ATTEMPTS,
                        jobId
                );

                return restTemplate.postForEntity(
                        moduleBaseUrl + "/scan",
                        request,
                        ModuleScanResponse.class
                );

            } catch (ResourceAccessException e) {
                lastException = e;

                log.warn(
                        "No se pudo conectar con el módulo. Intento {}/{}",
                        attempt,
                        MAX_MODULE_START_ATTEMPTS
                );

                if (attempt < MAX_MODULE_START_ATTEMPTS) {
                    esperarRetry(attempt);
                }
            }
        }

        throw new ResourceAccessException(
                "No se pudo conectar con el módulo después de "
                        + MAX_MODULE_START_ATTEMPTS + " intentos"
        );
    }

    private void validarRespuestaModulo(ResponseEntity<ModuleScanResponse> response) {

        if (response.getStatusCode() != HttpStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "El módulo rechazó el inicio del escaneo. HTTP " + response.getStatusCode()
            );
        }

        ModuleScanResponse body = response.getBody();

        if (body == null) {
            throw new IllegalStateException(
                    "El módulo no devolvió información al iniciar el escaneo"
            );
        }

        if (body.scanId() == null || body.scanId().isBlank()) {
            throw new IllegalStateException(
                    "El módulo no devolvió scan_id en la respuesta"
            );
        }
    }

    private void esperarRetry(int attempt) {

        try {
            Thread.sleep(MODULE_RETRY_DELAY_MS * attempt);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "El inicio del escaneo fue interrumpido",
                    e
            );
        }
    }
}

