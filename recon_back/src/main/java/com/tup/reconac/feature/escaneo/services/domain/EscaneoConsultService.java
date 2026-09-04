package com.tup.reconac.feature.escaneo.services.domain;

import com.tup.reconac.exceptions.global.BadRequestException;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.dtos.EscaneoListadoResponse;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EscaneoConsultService {

    private final EscaneoRepository repo;
    private final AuditoriaConsultService auditoriaConsult;
    private final UserDetailsService userService;

    public void verifyAuditoriaOwnership(UUID auditoriaId) {
        Auditoria auditoria = auditoriaConsult.findId(auditoriaId);
        Usuario usuario = userService.getAuthenticatedUser();
        if (!auditoria.getUsuarioId().equals(usuario.getId())) {
            throw new BadRequestException("La auditoría no pertenece al usuario autenticado");
        }
    }

    public Escaneo findAndVerify(UUID auditoriaId, UUID escaneoId) {
        verifyAuditoriaOwnership(auditoriaId);
        Escaneo escaneo = repo.findById(escaneoId)
                .orElseThrow(() -> new BadRequestException("Escaneo no encontrado: " + escaneoId));
        if (!escaneo.getAuditoriaId().equals(auditoriaId)) {
            throw new BadRequestException("El escaneo no pertenece a la auditoría indicada");
        }
        return escaneo;
    }

    @Transactional
    public void delete(UUID auditoriaId, UUID escaneoId) {
        findAndVerify(auditoriaId, escaneoId);
        repo.deleteById(escaneoId);
    }

    public Page<EscaneoListadoResponse> findAllForAuthenticatedUser(Pageable pageable) {

        Usuario usuario = userService.getAuthenticatedUser();

        List<Auditoria> auditorias =
                auditoriaConsult.findAllByUsuarioId(usuario.getId());

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
