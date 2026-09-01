package com.tup.reconac.feature.activo.controllers;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.activo.dtos.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.ActivoResponse;
import com.tup.reconac.feature.activo.services.interfaces.IActivoCreateService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activos")
@AllArgsConstructor
public class ActivoPostController {

    private final IActivoCreateService activoCreate;

    @PostMapping
    public ResponseEntity<BaseResponse<ActivoResponse>> create(
            @Valid @RequestBody ActivoRequestDto dto
    ) {
        ActivoResponse response = activoCreate.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.ok(response, "Activo creado correctamente")
        );
    }
}
