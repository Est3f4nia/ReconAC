package com.tup.reconac.feature.escaneo.services.domain;

import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoListadoResponse;
import com.tup.reconac.feature.escaneo.mappers.EscaneoMapper;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.usuario.services.domain.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EscaneoConsultService {

    private final EscaneoRepository repo;
    private final AuditoriaConsultService auditoriaConsult;
    private final CurrentUserService currentUser;

    // Getters ---

    public List<UUID> getEscaneoIds(Set<UUID> auditoriaIds) {
        return repo.findIdsByAuditoriaIdIn(auditoriaIds);
    }

    public List<UUID> getEscaneoIds(UUID auditoriaId) {return repo.findIdsByAuditoriaId(auditoriaId); }

    public List<Escaneo> getEscaneos(UUID auditoriaId) {
        return repo.findByAuditoriaIdOrderByCreadoADesc(auditoriaId);
    }

    public Optional<Escaneo> getUltimoEscaneo(UUID auditoriaId) {
        return repo.findFirstByAuditoriaIdOrderByCreadoADesc(auditoriaId);
    }

    // Finders ---
    // Pueden hacerse métodos void para verificar y separar funciones (find para verificar)

    // Búsqueda directa, sin validación
    public Escaneo findById(UUID escaneoId) {
        return repo.findById(escaneoId)
                .orElseThrow(() ->
                        new EscaneoNotFoundException("Escaneo no encontrado: " + escaneoId));
    }

    // Búsqueda con validación de pertenencia a auditoria
    // + validación de pertenencia de auditoría a usuario
    public Escaneo findEscaneoForAuditoria(UUID auditoriaId, UUID escaneoId) {

        auditoriaConsult.verifyAuditoriaOwnership(auditoriaId);

        return repo.findByIdAndAuditoriaId(escaneoId, auditoriaId)
                .orElseThrow(() ->
                        new EscaneoNotFoundException(
                                "Escaneo no encontrado (findEscaneoForAuditoria): " + escaneoId
                        )
                );
    }

    // Búsqueda con validación de pertenencia a usuario
    public Escaneo findEscaneoForUsuario(UUID escaneoId) {

        UUID usuarioId = currentUser.getUsuarioId();

        Escaneo escaneo = repo.findById(escaneoId)
                .orElseThrow(() -> new EscaneoNotFoundException("Escaneo no encontrado: " + escaneoId));
        auditoriaConsult.verifyEscaneoOwnership(escaneo, usuarioId);
        return escaneo;
    }

    public Page<EscaneoListadoResponse> findAllForAuthenticatedUser(Pageable pageable) {

        UUID usuarioId = currentUser.getUsuarioId();

        List<Auditoria> auditorias = auditoriaConsult.findAllByUsuarioId(usuarioId);
        if (auditorias.isEmpty()) return Page.empty(pageable);

        List<UUID> auditoriaIds = auditorias.stream()
                .map(Auditoria::getId)
                .toList();

        Map<UUID, String> nombresAuditorias = auditorias.stream()
                .collect(Collectors.toMap(
                        Auditoria::getId,
                        auditoria -> auditoria.getNombre() != null
                                ? auditoria.getNombre()
                                : "Nueva auditoría"
                ));

        Page<Escaneo> escaneos = repo.findByAuditoriaIdIn(auditoriaIds, pageable);

        return escaneos.map(escaneo ->
                EscaneoMapper.toListadoResponse(
                        escaneo,
                        nombresAuditorias.get(escaneo.getAuditoriaId())
                )
        );
    }
}
