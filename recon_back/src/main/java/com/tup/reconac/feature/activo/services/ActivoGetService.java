package com.tup.reconac.feature.activo.services;

import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.activo.dtos.ActivoAgrupadoResponse;
import com.tup.reconac.feature.activo.dtos.ActivoResponse;
import com.tup.reconac.feature.activo.mappers.ActivoMapper;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.activo.services.interfaces.IActivoGetService;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ActivoGetService implements IActivoGetService {

    private final ActivoRepository repo;
    private final UserDetailsService userService;
    private final AuditoriaConsultService auditoriaConsult;
    private final EscaneoRepository escaneoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ActivoAgrupadoResponse> getAll(Pageable pageable) {

        Usuario usuario = userService.getAuthenticatedUser();

        List<Auditoria> auditorias =
                auditoriaConsult.findAllByUsuarioId(usuario.getId());

        if (auditorias.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> auditoriaIds = auditorias.stream()
                .map(Auditoria::getId)
                .toList();

        List<UUID> escaneoIds = escaneoRepository
                .findByAuditoriaIdIn(auditoriaIds, Pageable.unpaged())
                .stream()
                .map(Escaneo::getId)
                .toList();

        if (escaneoIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // Primero trae todos los activos de usuario. Por los casos de uso pensados, no debería perjudicar
        // mucho al rendimiento
        List<Activo> activos = repo.findByEscaneoIdIn(escaneoIds);

        Map<ActivoKey, List<Activo>> grupos = activos.stream()
                .collect(Collectors.groupingBy(activo ->
                        new ActivoKey(
                                activo.getHost(),
                                activo.getHostname(),
                                activo.getSo()
                        )
                ));

        List<ActivoAgrupadoResponse> agrupados = grupos.values()
                .stream()
                .map(ActivoMapper::toAgrupadoResponse)
                .toList();

        int start = Math.min(
                pageable.getPageNumber() * pageable.getPageSize(),
                agrupados.size()
        );

        int end = Math.min(
                start + pageable.getPageSize(),
                agrupados.size()
        );

        return new PageImpl<>(
                agrupados.subList(start, end),
                pageable,
                agrupados.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivoResponse> getByEscaneoId(
            UUID escaneoId,
            Pageable pageable
    ) {

        Usuario usuario = userService.getAuthenticatedUser();

        Escaneo escaneo = escaneoRepository.findById(escaneoId)
                .orElseThrow(() ->
                        new EscaneoNotFoundException(
                                "Escaneo no encontrado"
                        )
                );

        auditoriaConsult.verifyEscaneoOwnership(
                escaneo,
                usuario.getId()
        );

        return repo.findByEscaneoId(escaneoId, pageable)
                .map(ActivoMapper::toResponse);
    }

    private record ActivoKey(
            String host,
            String hostname,
            String so
    ) {}
}
