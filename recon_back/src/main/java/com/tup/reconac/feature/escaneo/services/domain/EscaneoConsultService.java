package com.tup.reconac.feature.escaneo.services.domain;

import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoListadoResponse;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EscaneoConsultService {

    private final EscaneoRepository repo;
    private final AuditoriaConsultService auditoriaConsult;
    private final UserDetailsService userService;

    // Getters ---

    public List<UUID> getEscaneoIds(List<UUID> auditoriaIds) {
        return repo.findByAuditoriaIdIn(auditoriaIds, Pageable.unpaged())
                .stream()
                .map(Escaneo::getId)
                .toList();
    }

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
                        new EscaneoNotFoundException("Escaneo no encontrado" + escaneoId));
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
    public Escaneo findEscaneoForUsuario(UUID escaneoId, UUID usuarioId) {
        Escaneo escaneo = repo.findById(escaneoId)
                .orElseThrow(() -> new EscaneoNotFoundException("Escaneo no encontrado: " + escaneoId));
        auditoriaConsult.verifyEscaneoOwnership(escaneo, usuarioId);
        return escaneo;
    }

    public Page<EscaneoListadoResponse> findAllForAuthenticatedUser(Pageable pageable) {

        Usuario usuario = userService.getAuthenticatedUser();

        List<Auditoria> auditorias = auditoriaConsult.findAllByUsuarioId(usuario.getId());
        if (auditorias.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> auditoriaIds = auditorias.stream()
                .map(Auditoria::getId)
                .toList();

        Map<UUID, String> nombresAuditorias = auditorias.stream()
                .collect(Collectors.toMap(
                        Auditoria::getId,
                        Auditoria::getNombre
                ));

//        return repo.findByAuditoriaIdIn(auditoriaIds, pageable)
//                .map(EscaneoMapper::toResponseListado);

        // mappeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeer
        return repo.findByAuditoriaIdIn(auditoriaIds, pageable)
                .map(escaneo -> new EscaneoListadoResponse(
                        escaneo.getId(),
                        escaneo.getAuditoriaId(),
                        nombresAuditorias.get(escaneo.getAuditoriaId()),
                        List.of(escaneo.getObjetivos()),
                        escaneo.getEstado(),
                        escaneo.getProgreso(),
                        escaneo.getNmapVersion(),
                        escaneo.getIniciadoA(),
                        escaneo.getCompletadoA()
                ));
    }
}
