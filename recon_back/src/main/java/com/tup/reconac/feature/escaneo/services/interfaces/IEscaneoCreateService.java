package com.tup.reconac.feature.escaneo.services.interfaces;

import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStartRequest;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStatusResponse;

import java.util.UUID;

public interface IEscaneoCreateService {
    EscaneoResponse startScan(UUID auditoriaId, ScanStartRequest req);

    void updateStatusFromExternal(String jobId, ScanStatusResponse status);

    void processCallback(String jobId, EscaneoResult result);
}
