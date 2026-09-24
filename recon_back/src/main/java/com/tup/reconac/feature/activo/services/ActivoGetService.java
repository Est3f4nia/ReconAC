package com.tup.reconac.feature.activo.services;

import com.tup.reconac.feature.activo.dtos.response.ActivoAgrupadoResponse;
import com.tup.reconac.feature.activo.dtos.response.ActivoResponse;
import com.tup.reconac.feature.activo.mappers.ActivoMapper;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.activo.services.interfaces.IActivoGetService;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.usuario.services.domain.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivoGetService implements IActivoGetService {

    private final ActivoRepository repo;
    private final CurrentUserService currentUser;
    private final AuditoriaConsultService auditoriaConsult;
    private final EscaneoConsultService escaneoConsult;

    @Override
    @Transactional(readOnly = true)
    public Page<ActivoAgrupadoResponse> getAll(Pageable pageable) {

        UUID usuarioId = currentUser.getUsuarioId();

        List<Auditoria> auditorias = auditoriaConsult.findAllByUsuarioId(usuarioId);
        if (auditorias.isEmpty()) return Page.empty(pageable);

        Set<UUID> auditoriaIds = auditorias.stream()
                .map(Auditoria::getId)
                .collect(Collectors.toSet());

        List<UUID> escaneoIds = escaneoConsult.getEscaneoIds(auditoriaIds);
        if (escaneoIds.isEmpty()) return Page.empty(pageable);

        List<Activo> activos = repo.findByEscaneoIdIn(escaneoIds);

        Map<ActivoKey, List<Activo>> grupos = activos.stream()
                .collect(Collectors.groupingBy(ActivoGetService::toActivoKey));

        List<ActivoAgrupadoResponse> agrupados = grupos.values().stream()
                .map(ActivoMapper::toAgrupadoResponse)
                .toList();

        int start = (int) Math.min(pageable.getOffset(), agrupados.size());
        int end = Math.min(start + pageable.getPageSize(), agrupados.size());

        return new PageImpl<>(
                agrupados.subList(start, end),
                pageable,
                agrupados.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivoResponse> getByEscaneoId(UUID escaneoId, Pageable pageable) {

        escaneoConsult.findEscaneoForUsuario(escaneoId);

        return repo.findByEscaneoId(escaneoId, pageable)
                .map(ActivoMapper::toResponse);
    }

    private static ActivoKey toActivoKey(Activo activo) {
        return new ActivoKey(
                activo.getHost(),
                activo.getHostname(),
                activo.getSo()
        );
    }

    private record ActivoKey(
            String host,
            String hostname,
            String so
    ) {}
}
