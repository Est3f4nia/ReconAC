package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResumenDto;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResult;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class EscaneoResumenService {

    private final EscaneoRepository escaneoRepo;
    private final AuditoriaRepository auditoriaRepo;
    private final UserDetailsService userService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<EscaneoResumenDto> getResumen() {
        UUID usuarioId = userService.getAuthenticatedUser().getId();
        List<Escaneo> escaneos = escaneoRepo.findResumenByUsuarioId(usuarioId);

        Map<UUID, Escaneo> latestByAuditoria = new LinkedHashMap<>();
        for (Escaneo e : escaneos) {
            latestByAuditoria.putIfAbsent(e.getAuditoriaId(), e);
        }

        List<EscaneoResumenDto> result = new ArrayList<>();
        for (Map.Entry<UUID, Escaneo> entry : latestByAuditoria.entrySet()) {
            UUID auditoriaId = entry.getKey();
            Escaneo escaneo = entry.getValue();

            Auditoria auditoria = auditoriaRepo.findById(auditoriaId).orElse(null);
            String nombre = auditoria != null ? auditoria.getNombre() : "Sin nombre";

            int activos = 0;
            int puertos = 0;
            int cve = 0;
            int cveCriticos = 0;

            if (escaneo.getResultado() != null && !escaneo.getResultado().isBlank()) {
                try {
                    EscaneoResult res = objectMapper.readValue(
                            escaneo.getResultado(), EscaneoResult.class);

                    if (res.hosts() != null) {
                        activos = res.hosts().size();
                    }

                    if (res.apiResults() != null) {
                        Object portsObj = res.apiResults().get("ports");
                        if (portsObj instanceof Number n) {
                            puertos = n.intValue();
                        }

                        Object cvesObj = res.apiResults().get("cves");
                        if (cvesObj instanceof List<?> cvesList) {
                            cve = cvesList.size();
                            for (Object c : cvesList) {
                                if (c instanceof Map<?, ?> map) {
                                    Object sev = map.get("severidad");
                                    if ("CRITICA".equalsIgnoreCase(String.valueOf(sev))) {
                                        cveCriticos++;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            result.add(new EscaneoResumenDto(
                    escaneo.getId(),
                    auditoriaId,
                    nombre,
                    activos,
                    puertos,
                    escaneo.getCompletadoA(),
                    escaneo.getEstado(),
                    cve,
                    cveCriticos
            ));
        }

        return result;
    }
}
