package com.tup.reconac.feature.auditoria.services.interfaces;

import com.tup.reconac.feature.auditoria.dtos.AuditoriaResponse;

import java.util.List;

public interface IAuditoriaGetService {
    List<AuditoriaResponse> getAll();
}
