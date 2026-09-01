package com.tup.reconac.feature.escaneo.services.interfaces;

import com.tup.reconac.feature.escaneo.dtos.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.ScanStartRequest;
import com.tup.reconac.feature.escaneo.dtos.ScanStatusResponse;

import java.util.UUID;

public interface IEscaneoPostService {
    EscaneoResponse startScan(UUID auditoriaId, ScanStartRequest req);

    void updateStatusFromExternal(String jobId, ScanStatusResponse status);

    void processCallback(String jobId, EscaneoResult result);
}
