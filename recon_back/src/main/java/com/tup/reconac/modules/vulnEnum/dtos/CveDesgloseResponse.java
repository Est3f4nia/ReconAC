package com.tup.reconac.modules.vulnEnum.dtos;

import java.util.List;

public record CveDesgloseResponse(
        List<CveResumenResponse> masComunes,
        List<CveResumenResponse> explotacionActiva,
        List<CveResumenResponse> mayorCriticidad,
        List<CveResumenResponse> mayorProbabilidadExplotacion
) {}
