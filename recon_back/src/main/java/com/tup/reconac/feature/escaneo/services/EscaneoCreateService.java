package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.exceptions.global.BadRequestException;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.response.HostResult;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStartRequest;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.mappers.EscaneoMapper;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoPostService;
import com.tup.reconac.feature.puerto.dtos.PuertoResultadoResponse;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.feature.puerto.repositories.PuertoRepository;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import com.tup.reconac.config.ModulesConfig;
import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.mappers.CpeParser;
import com.tup.reconac.modules.vulnEnum.models.PuertoCpe;
import com.tup.reconac.modules.vulnEnum.repositories.CpeRepository;
import com.tup.reconac.modules.vulnEnum.repositories.PuertoCpeRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class EscaneoCreateService implements IEscaneoPostService {

    private final EscaneoRepository repo;
    private final ActivoRepository activoRepo;
    private final PuertoRepository puertoRepo;
    private final AuditoriaConsultService auditoriaConsult;
    private final UserDetailsService userService;
    private final ModulesConfig modulesConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EscaneoPersistenceService persistenceService;
    private final CpeRepository cpeRepository;
    private final PuertoCpeRepository puertoCpeRepository;
    private final com.tup.reconac.modules.vulnEnum.services.VulnerabilityCatalogLock catalogLock;

    private static final int MAX_MODULE_START_ATTEMPTS = 3;
    private static final long MODULE_RETRY_DELAY_MS = 500;

    @Override
    public EscaneoResponse startScan(UUID auditoriaId, ScanStartRequest req) {
        Auditoria auditoria = auditoriaConsult.findId(auditoriaId);
        Usuario usuario = userService.getAuthenticatedUser();

        if (!auditoria.getUsuarioId().equals(usuario.getId())) {
            throw new BadRequestException("La auditoría no pertenece al usuario autenticado");
        }


        String jobId = UUID.randomUUID().toString();

        Escaneo saved = persistenceService.crear(
                auditoriaId,
                req.objetivos().toArray(new String[0]),
                jobId
        );

        try {
            String moduleBaseUrl = modulesConfig.getEndpoint("recon").getUrl();

            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("job_id", jobId);
            body.put("escaneo_id", saved.getId().toString());
            body.put("targets", req.objetivos());
            body.put("timeout", req.timeout() != null ? req.timeout() : 600);
            body.put("icmp_timeout", req.icmpTimeout() != null ? req.icmpTimeout() : 5);
            body.put("max_cve_years", req.maxCveYears() != null ? req.maxCveYears() : 2);
            body.put("min_cvss_score", req.minCvssScore() != null ? req.minCvssScore() : 0.0);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = null;
            ResourceAccessException lastConnectionException = null;

            for (int attempt = 1; attempt <= MAX_MODULE_START_ATTEMPTS; attempt++) {
                try {
                    System.out.println(
                            "INICIANDO ESCANEO EN MÓDULO -> intento "
                                    + attempt
                                    + "/"
                                    + MAX_MODULE_START_ATTEMPTS
                                    + " jobId="
                                    + jobId
                    );

                    response = restTemplate.postForEntity(
                            moduleBaseUrl + "/scan",
                            request,
                            Map.class
                    );

                    break;

                } catch (ResourceAccessException e) {
                    lastConnectionException = e;

                    System.out.println(
                            "ADVERTENCIA: no se pudo conectar con el módulo "
                                    + "(intento "
                                    + attempt
                                    + "/"
                                    + MAX_MODULE_START_ATTEMPTS
                                    + ")"
                    );

                    if (attempt < MAX_MODULE_START_ATTEMPTS) {
                        try {
                            Thread.sleep(MODULE_RETRY_DELAY_MS * attempt);
                        } catch (InterruptedException interruptedException) {
                            Thread.currentThread().interrupt();

                            throw new IllegalStateException(
                                    "El inicio del escaneo fue interrumpido",
                                    interruptedException
                            );
                        }
                    }
                }
            }

            if (response == null) {
                throw new ResourceAccessException(
                        "No se pudo conectar con el módulo después de "
                                + MAX_MODULE_START_ATTEMPTS
                                + " intentos"
                );
            }

            if (response.getStatusCode() != HttpStatus.ACCEPTED) {
                throw new IllegalStateException(
                        "El módulo rechazó el inicio del escaneo. "
                                + "HTTP " + response.getStatusCode()
                );
            }

            if (response.getBody() == null) {
                throw new IllegalStateException(
                        "El módulo no devolvió información al iniciar el escaneo"
                );
            }

            Object scanIdObj = response.getBody().get("scan_id");

            if (scanIdObj == null) {
                throw new IllegalStateException(
                        "El módulo no devolvió scan_id en la respuesta"
                );
            }

            /*
             * jobId es el identificador de correlación generado por el backend.
             * Python debe utilizar este mismo identificador para status y callback.
             */
            saved.setModuloJobId(jobId);
            saved.setEstado(EscaneoEstado.EN_PROCESO);
            saved.setIniciadoA(LocalDateTime.now());

            repo.save(saved);

        } catch (ResourceAccessException e) {

            saved.setEstado(EscaneoEstado.FALLO);
            saved.setMensajeError(
                    "No se pudo conectar con el módulo de reconocimiento después de "
                            + MAX_MODULE_START_ATTEMPTS
                            + " intentos: "
                            + e.getMessage()
            );

            repo.save(saved);

        } catch (Exception e) {

            saved.setEstado(EscaneoEstado.FALLO);
            saved.setMensajeError(
                    "Error al iniciar el escaneo: " + e.getMessage()
            );

            repo.save(saved);
        }

        return EscaneoMapper.toResponse(saved);
    }

    @Override
    public void updateStatusFromExternal(String jobId, ScanStatusResponse status) {

        Escaneo escaneo = repo.findByModuloJobId(jobId)
                .orElseThrow(() -> new BadRequestException("Escaneo no encontrado para jobId: " + jobId));

        if (status.status() != null) {
            escaneo.setEstado(status.status());
        }
        if (status.progress() != null) {
            escaneo.setProgreso(status.progress());
        }
        if (status.error() != null) {
            escaneo.setMensajeError(status.error());
        }
        if (EscaneoEstado.COMPLETADO.equals(status.status()) || EscaneoEstado.FALLO.equals(status.status())) {
            escaneo.setCompletadoA(LocalDateTime.now());
        }
        repo.save(escaneo);
    }

    @Override
    @Transactional
    public void processCallback(String jobId, EscaneoResult result) {

        // ver esto
        Optional<Escaneo> encontrado = Optional.of(repo.findByModuloJobId(jobId)
                .orElseThrow(() ->
                        new EscaneoNotFoundException("Escaneo no encontrado para jobId: " + jobId)
                ));

        try {
            Escaneo escaneo = repo.findByModuloJobId(jobId)
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "Escaneo no encontrado para jobId: " + jobId
                            )
                    );

            escaneo.setResultado(
                    objectMapper.writeValueAsString(result)
            );

            escaneo.setNmapVersion(result.nmapVersion());

            migrarActivos(
                    escaneo,
                    result.hosts()
            );

            escaneo.setEstado(EscaneoEstado.COMPLETADO);
            escaneo.setProgreso(100);
            escaneo.setCompletadoA(LocalDateTime.now());

            repo.save(escaneo);

        } catch (BadRequestException e) {
            throw e;

        } catch (Exception e) {
            Escaneo escaneo = repo.findByModuloJobId(jobId)
                    .orElse(null);

            if (escaneo != null) {
                escaneo.setEstado(EscaneoEstado.FALLO);
                escaneo.setMensajeError(
                        "Error al procesar el resultado del escaneo: "
                                + e.getMessage()
                );
                repo.save(escaneo);
            }

            e.printStackTrace();
            throw new RuntimeException(
                    "Error procesando callback del escaneo " + jobId,
                    e
            );
        }
    }

    // activoConsultService
    private void migrarActivos(
            Escaneo escaneo,
            List<HostResult> hosts
    ) {
        for (HostResult host : hosts) {

            Activo activo = new Activo();
            activo.setEscaneoId(escaneo.getId());
            activo.setHost(host.ip());
            activo.setMac(host.mac());
            activo.setHostname(host.hostname());
            activo.setSo(host.os());
            activo.setSoProbab(host.soProbab());

            activo = activoRepo.save(activo);

            if (host.puertos() == null || host.puertos().isEmpty()) {
                continue;
            }

            for (PuertoResultadoResponse resultadoPuerto : host.puertos()) {

                Puerto puerto = new Puerto();

                puerto.setActivoId(activo.getId());
                puerto.setNumero(resultadoPuerto.numero());
                puerto.setProtocolo(resultadoPuerto.protocolo());
                puerto.setEstado(resultadoPuerto.estado());

                puerto.setServicioFallback(
                        resultadoPuerto.servicio()
                );

                puerto = puertoRepo.save(puerto);

                migrarCpes(puerto, resultadoPuerto.cpes());
            }
        }
    }

    private void migrarCpes(
            Puerto puerto,
            List<String> cpes
    ) {
        if (cpes == null || cpes.isEmpty()) {
            return;
        }
        catalogLock.acquire();

        for (String uri : cpes) {

            if (uri == null || uri.isBlank()) {
                continue;
            }

            Cpe cpe = cpeRepository.findByUri(uri)
                    .orElseGet(() -> crearCpe(uri));

            CpeParser.enrich(cpe);
            cpeRepository.save(cpe);

            if (!puertoCpeRepository.existsByPuertoIdAndCpeId(
                    puerto.getId(),
                    cpe.getId()
            )) {
                PuertoCpe puertoCpe = new PuertoCpe();
                puertoCpe.setPuertoId(puerto.getId());
                puertoCpe.setCpeId(cpe.getId());

                puertoCpeRepository.save(puertoCpe);
            }
        }
    }

    private Cpe crearCpe(String uri) {
        Cpe cpe = new Cpe();

        cpe.setUri(uri);
        cpe.setUriLegible(uri);
        CpeParser.enrich(cpe);
        cpe.setUltimoCheck(null);

        return cpeRepository.save(cpe);
    }
}

