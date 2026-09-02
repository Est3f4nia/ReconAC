package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.exceptions.global.BadRequestException;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.HostResult;
import com.tup.reconac.feature.escaneo.dtos.ScanStartRequest;
import com.tup.reconac.feature.escaneo.dtos.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.mappers.EscaneoMapper;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoPostService;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import com.tup.reconac.feature.usuario.services.domain.UsuarioKeyService;
import com.tup.reconac.config.ModulesConfig;
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
import java.util.UUID;

@Service
@AllArgsConstructor
public class EscaneoCreateService implements IEscaneoPostService {

    private final EscaneoRepository repo;
    private final ActivoRepository activoRepo;
    private final AuditoriaConsultService auditoriaConsult;
    private final UserDetailsService userService;
    private final UsuarioKeyService keyService;
    private final ModulesConfig modulesConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public EscaneoResponse startScan(UUID auditoriaId, ScanStartRequest req) {
        Auditoria auditoria = auditoriaConsult.findId(auditoriaId);
        Usuario usuario = userService.getAuthenticatedUser();

        if (!auditoria.getUsuarioId().equals(usuario.getId())) {
            throw new BadRequestException("La auditoría no pertenece al usuario autenticado");
        }

        String providedKey = req.nvdApiKey();
        boolean userHasKey = usuario.getNvdApiKeyHash() != null;
        if (providedKey == null || providedKey.isBlank()) {
            if (userHasKey) {
                throw new BadRequestException("Debe enviar su NVD API key para enriquecer el escaneo con CVEs");
            }
        } else if (userHasKey && !keyService.isUserKey(usuario, providedKey)) {
            throw new BadRequestException("La NVD API key no corresponde al usuario registrado");
        }

        Escaneo escaneo = new Escaneo();
        escaneo.setAuditoriaId(auditoriaId);
        escaneo.setObjetivos(req.objetivos().toArray(new String[0]));
        escaneo.setEstado(EscaneoEstado.PENDIENTE);

        Escaneo saved = repo.save(escaneo);

        try {
            /*
             * Opción B (futura, ADR-013 / ADR-010): descubrimiento dinámico vía registro en
             * DB/Redis. En lugar de resolver por config estática, se consultaría una tabla
             * `modulo` (activo=true) cacheada en Redis:
             *     String moduleBaseUrl = moduleRegistry.resolveUrl("recon");
             * La interfaz de resolución se mantendría igual, por lo que este call site no
             * cambiaría. Por ahora se usa la config externalizada (Opción A).
             */
            String moduleBaseUrl = modulesConfig.getEndpoint("recon").getUrl();

            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("targets", req.objetivos());
            body.put("timeout", req.timeout() != null ? req.timeout() : 600);
            body.put("icmp_timeout", req.icmpTimeout() != null ? req.icmpTimeout() : 5);
            body.put("max_cve_years", req.maxCveYears() != null ? req.maxCveYears() : 2);
            body.put("min_cvss_score", req.minCvssScore() != null ? req.minCvssScore() : 0.0);
            if (providedKey != null && !providedKey.isBlank()) {
                body.put("nvd_api_key", providedKey);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    moduleBaseUrl + "/scan", request, Map.class);

            if (response.getStatusCode() == HttpStatus.ACCEPTED && response.getBody() != null) {
                String externalScanId = (String) response.getBody().get("scan_id");
                saved.setModuloJobId(externalScanId);
                saved.setEstado(EscaneoEstado.EN_PROCESO);
                saved.setIniciadoA(LocalDateTime.now());
                repo.save(saved);
            } else {
                saved.setEstado(EscaneoEstado.FALLO);
                saved.setMensajeError("El módulo no pudo iniciar el escaneo");
                repo.save(saved);
            }
        } catch (ResourceAccessException e) {
            saved.setEstado(EscaneoEstado.FALLO);
            saved.setMensajeError("No se pudo conectar con el módulo de reconocimiento: " + e.getMessage());
            repo.save(saved);
        } catch (Exception e) {
            saved.setEstado(EscaneoEstado.FALLO);
            saved.setMensajeError("Error al iniciar el escaneo: " + e.getMessage());
            repo.save(saved);
        }

        return EscaneoMapper.toResponse(saved);
    }

    @Override
    @Transactional
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
        Escaneo escaneo = repo.findByModuloJobId(jobId)
                .orElseThrow(() -> new BadRequestException("Escaneo no encontrado para jobId: " + jobId));

        try {
            escaneo.setResultado(objectMapper.writeValueAsString(result));
            escaneo.setNmapVersion(result.nmapVersion());
            escaneo.setEstado(EscaneoEstado.COMPLETADO);
            escaneo.setProgreso(100);
            escaneo.setCompletadoA(LocalDateTime.now());
            repo.save(escaneo);

            migrarActivos(escaneo, result.hosts());
        } catch (Exception e) {
            escaneo.setEstado(EscaneoEstado.FALLO);
            escaneo.setMensajeError("Error al procesar el resultado del escaneo: " + e.getMessage());
            repo.save(escaneo);
        }
    }

    private void migrarActivos(Escaneo escaneo, List<HostResult> hosts) {
        if (hosts == null || hosts.isEmpty()) {
            return;
        }
        for (HostResult host : hosts) {
            Activo activo = new Activo();
            activo.setEscaneoId(escaneo.getId());
            activo.setHost(host.ip());
            activo.setMac(host.mac());
            activo.setHostname(host.hostname());
            activo.setSo(host.os());
            activoRepo.save(activo);
        }
    }
}
