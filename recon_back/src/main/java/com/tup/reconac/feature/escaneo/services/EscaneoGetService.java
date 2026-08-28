package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.feature.escaneo.dtos.EscaneoResultResponse;
import com.tup.reconac.feature.escaneo.dtos.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.mappers.EscaneoMapper;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoGetService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class EscaneoGetService implements IEscaneoGetService {

    private final EscaneoConsultService consult;

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
        return new EscaneoResultResponse(EscaneoMapper.toResponse(escaneo), escaneo.getResultado());
    }
}
