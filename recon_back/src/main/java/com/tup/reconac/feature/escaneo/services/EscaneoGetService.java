package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.activo.dtos.response.ActivoResultadoResponse;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.mappers.EscaneoMapper;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoGetService;
import com.tup.reconac.feature.puerto.dtos.PuertoResultadoResponse;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.feature.puerto.repositories.PuertoRepository;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.models.PuertoCpe;
import com.tup.reconac.modules.vulnEnum.repositories.CpeRepository;
import com.tup.reconac.modules.vulnEnum.repositories.PuertoCpeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class EscaneoGetService implements IEscaneoGetService {

    private final EscaneoConsultService escaneoConsult;
    private final UserDetailsService userService;
    private final EscaneoRepository repo;
    private final AuditoriaConsultService auditoriaConsult;

    private final ActivoRepository activoRepo;
    private final PuertoRepository puertoRepo;
    private final PuertoCpeRepository puertoCpeRepo;
    private final CpeRepository cpeRepo;

    @Override
    @Transactional(readOnly = true)
    public ScanStatusResponse getStatus(
            UUID auditoriaId,
            UUID escaneoId
    ) {
        Escaneo escaneo =
                escaneoConsult.findEscaneoForAuditoria(
                        auditoriaId,
                        escaneoId
                );


        return EscaneoMapper.toStatusResponse(escaneo);
    }

    @Override
    @Transactional(readOnly = true)
    public EscaneoResult.EscaneoResultResponse getResultado(
            UUID auditoriaId,
            UUID escaneoId
    ) {
        Escaneo escaneo =
                escaneoConsult.findEscaneoForAuditoria(
                        auditoriaId,
                        escaneoId
                );

        List<Activo> activos =
                activoRepo.findByEscaneoId(escaneoId);

        List<ActivoResultadoResponse> activosResponse =
                activos.stream()
                        .map(this::toActivoResultado)
                        .toList();

        // mapeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeer
        return new EscaneoResult.EscaneoResultResponse(
                escaneo.getId(),
                escaneo.getEstado(),
                escaneo.getProgreso(),
                escaneo.getNmapVersion(),
                escaneo.getIniciadoA(),
                escaneo.getCompletadoA(),
                activosResponse
        );
    }

    @Override
    @Transactional(readOnly = true)
    public EscaneoResult.EscaneoResultResponse getById(UUID escaneoId) {

        UUID usuarioId =
                userService.getAuthenticatedUser().getId();

        Escaneo escaneo =
                repo.findById(escaneoId)
                        .orElseThrow(() ->
                                new EscaneoNotFoundException(
                                        "Escaneo no encontrado get s"
                                )
                        );

        auditoriaConsult.verifyEscaneoOwnership(
                escaneo,
                usuarioId
        );

        List<Activo> activos =
                activoRepo.findByEscaneoId(escaneoId);

        List<ActivoResultadoResponse> activosResponse =
                activos.stream()
                        .map(this::toActivoResultado)
                        .toList();

        return new EscaneoResult.EscaneoResultResponse(
                escaneo.getId(),
                escaneo.getEstado(),
                escaneo.getProgreso(),
                escaneo.getNmapVersion(),
                escaneo.getIniciadoA(),
                escaneo.getCompletadoA(),
                activosResponse
        );
    }

    private ActivoResultadoResponse toActivoResultado(
            Activo activo
    ) {
        List<Puerto> puertos =
                puertoRepo.findByActivoId(activo.getId());

        List<PuertoResultadoResponse> puertosResponse =
                puertos.stream()
                        .map(this::toPuertoResultado)
                        .toList();

        return new ActivoResultadoResponse(
                activo.getId(),
                activo.getHost(),
                activo.getHostname(),
                activo.getSo(),
                activo.getSoProbab(),
                activo.getMac(),
                puertosResponse
        );
    }

    private PuertoResultadoResponse toPuertoResultado(
            Puerto puerto
    ) {
        List<PuertoCpe> relaciones =
                puertoCpeRepo.findByPuertoId(puerto.getId());

        List<String> cpes =
                relaciones.stream()
                        .map(PuertoCpe::getCpeId)
                        .map(cpeRepo::findById)
                        .flatMap(Optional::stream)
                        .map(Cpe::getUri)
                        .toList();

        return new PuertoResultadoResponse(
                puerto.getNumero(),
                puerto.getProtocolo(),
                puerto.getEstado(),
                puerto.getServicioFallback(),
                // ver esto para linkear cpes a puerto
                null,
                null,
                null,
                cpes
        );
    }
}
