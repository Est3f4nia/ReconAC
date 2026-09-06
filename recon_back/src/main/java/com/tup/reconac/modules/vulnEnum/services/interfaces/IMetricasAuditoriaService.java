package com.tup.reconac.modules.vulnEnum.services.interfaces;

import com.tup.reconac.modules.vulnEnum.dtos.DashboardAuditoriaResponse;

import java.util.UUID;

public interface IMetricasAuditoriaService {

    DashboardAuditoriaResponse getDashboard(UUID auditoriaId);
}
