package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.dtos.AuditoriaResumenDto;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaResumenService {

    private static final Logger log =
            LoggerFactory.getLogger(AuditoriaResumenService.class);

    private final AuditoriaRepository auditoriaRepo;
    private final EscaneoRepository escaneoRepo;
    private final UserDetailsService userService;
    private final ObjectMapper objectMapper;

    public List<AuditoriaResumenDto> getResumen() {

        UUID usuarioId = userService.getAuthenticatedUser().getId();
        log.info("getResumen() - usuarioId: {}", usuarioId);

        List<Auditoria> auditorias =
                auditoriaRepo.findByUsuarioIdOrderByFechaGeneracionDesc(usuarioId);

        log.info("auditorias found: {}", auditorias.size());

        List<AuditoriaResumenDto> result = new ArrayList<>();

        for (Auditoria auditoria : auditorias) {

            log.info(
                    "auditoria: {} ({})",
                    auditoria.getId(),
                    auditoria.getNombre()
            );

            Optional<Escaneo> ultimoEscaneo =
                    escaneoRepo.findFirstByAuditoriaIdOrderByCreadoADesc(
                            auditoria.getId()
                    );

            if (ultimoEscaneo.isEmpty()) {

                log.info(
                        "auditoria {} no tiene escaneos",
                        auditoria.getId()
                );

                result.add(
                        new AuditoriaResumenDto(
                                null,
                                auditoria.getId(),
                                auditoria.getNombre(),
                                0,
                                0,
                                null,
                                null,
                                0,
                                0
                        )
                );

                continue;
            }

            Escaneo escaneo = ultimoEscaneo.get();

            log.info(
                    "escaneo: {} estado: {}",
                    escaneo.getId(),
                    escaneo.getEstado()
            );

            int activos = 0;
            int puertos = 0;
            int cve = 0;
            int cveCriticos = 0;

            String resultado = escaneo.getResultado();

            if (resultado != null && !resultado.isBlank()) {

                JsonNode root = objectMapper.readTree(resultado);

                /*
                 * Hosts / activos
                 */
                JsonNode hosts = root.get("hosts");

                if (hosts != null && hosts.isArray()) {
                    activos = hosts.size();
                }

                /*
                 * Resultados de las APIs
                 */
                JsonNode apiResults = root.get("apiResults");

                if (apiResults != null && apiResults.isObject()) {

                    /*
                     * Puertos
                     */
                    JsonNode ports = apiResults.get("ports");

                    if (ports != null && ports.isNumber()) {
                        puertos = ports.asInt();
                    }

                    /*
                     * CVEs
                     */
                    JsonNode cves = apiResults.get("cves");

                    if (cves != null && cves.isArray()) {

                        cve = cves.size();

                        for (JsonNode cveNode : cves) {

                            JsonNode severidad =
                                    cveNode.get("severidad");

                            if (severidad != null
                                    && "CRITICA".equalsIgnoreCase(
                                    severidad.asText())) {

                                cveCriticos++;
                            }
                        }
                    }
                }
            }

            result.add(
                    new AuditoriaResumenDto(
                            escaneo.getId(),
                            auditoria.getId(),
                            auditoria.getNombre(),
                            activos,
                            puertos,
                            escaneo.getCompletadoA(),
                            escaneo.getEstado(),
                            cve,
                            cveCriticos
                    )
            );
        }

        log.info("getResumen() - result size: {}", result.size());

        return result;
    }
}