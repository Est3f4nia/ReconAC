package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResumenResponseDto;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaResumenService {

    private final AuditoriaRepository repo;
    private final EscaneoConsultService escaneoConsult;
    private final UserDetailsService userService;
    private final ObjectMapper objectMapper;

    public List<AuditoriaResumenResponseDto> getResumen() {

        UUID usuarioId = userService.getAuthenticatedUser().getId();
        List<Auditoria> auditorias = repo.findByUsuarioIdOrderByFechaGeneracionDesc(usuarioId);

        List<AuditoriaResumenResponseDto> result = new ArrayList<>();
        for (Auditoria auditoria : auditorias) {

            Optional<Escaneo> ultimoEscaneo = escaneoConsult.getUltimoEscaneo(auditoria.getId());

            if (ultimoEscaneo.isEmpty()) {
                result.add(
                        new AuditoriaResumenResponseDto(
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

            int activos = 0;
            int puertos = 0;
            int cve = 0;
            int cveCriticos = 0;

            String resultado = escaneo.getResultado();

            if (resultado != null && !resultado.isBlank()) {

                JsonNode root = objectMapper.readTree(resultado);

                // Activos ---

                JsonNode hosts = root.get("hosts");

                if (hosts != null && hosts.isArray()) {
                    activos = hosts.size();
                }

                // API results ---

                JsonNode apiResults = root.get("apiResults");
                if (apiResults != null && apiResults.isObject()) {

                    // Puertos ---

                    JsonNode ports = apiResults.get("ports");
                    if (ports != null && ports.isNumber()) {
                        puertos = ports.asInt();
                    }

                    // CVE ---

                    JsonNode cves = apiResults.get("cves");
                    if (cves != null && cves.isArray()) {
                        cve = cves.size();
                        for (JsonNode cveNode : cves) {
                            JsonNode severidad = cveNode.get("severidad");
                            if (severidad != null
                                    && "CRITICA".equalsIgnoreCase(
                                    severidad.asString())) {

                                cveCriticos++;
                            }
                        }
                    }
                }
            }

            result.add(
                    new AuditoriaResumenResponseDto(
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

        return result;
    }
}