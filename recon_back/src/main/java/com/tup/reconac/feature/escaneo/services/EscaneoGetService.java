package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResultResponse;
import com.tup.reconac.feature.escaneo.dtos.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.mappers.EscaneoMapper;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoGetService;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@AllArgsConstructor
public class EscaneoGetService implements IEscaneoGetService {

    private final EscaneoConsultService consult;
    private final UserDetailsService userService;
    private final EscaneoRepository repo;
    private final AuditoriaConsultService auditoriaConsult;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public ScanStatusResponse getStatus(UUID auditoriaId, UUID escaneoId) {
        Escaneo escaneo = consult.findAndVerify(auditoriaId, escaneoId);
        return EscaneoMapper.toStatusResponse(escaneo);
    }

    @Override
    @Transactional(readOnly = true)
    public EscaneoResultResponse getResultado(UUID auditoriaId, UUID escaneoId) {
        Escaneo escaneo = consult.findAndVerify(auditoriaId, escaneoId);
        return new EscaneoResultResponse(
                EscaneoMapper.toResponse(escaneo),
                parseResultado(escaneo.getResultado())
        );
    }

    @Override
    public EscaneoResultResponse getById(UUID escaneoId) {

        UUID usuarioId = userService.getAuthenticatedUser().getId();

        Escaneo escaneo = repo.findById(escaneoId)
                .orElseThrow(() ->
                        new EscaneoNotFoundException("Escaneo no encontrado")
                );

        auditoriaConsult.verifyEscaneoOwnership(escaneo, usuarioId);

        EscaneoResponse escaneoResponse = EscaneoMapper.toResponse(escaneo);

        EscaneoResult resultado = parseResultado(
                escaneo.getResultado()
        );

        return new EscaneoResultResponse(
                escaneoResponse,
                resultado
        );
    }

    private EscaneoResult parseResultado(String resultadoJson) {

        if (resultadoJson == null || resultadoJson.isBlank()) {
            return null;
        }

        return objectMapper.readValue(
                resultadoJson,
                EscaneoResult.class
        );
    }
}
