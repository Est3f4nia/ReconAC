package com.tup.reconac.feature.escaneo.services.interfaces;

import java.util.UUID;

public interface IEscaneoPatchService {
    void mover(UUID escaneoId, UUID nuevaAuditoriaId);
}
