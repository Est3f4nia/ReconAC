package com.tup.reconac.feature.activo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.server.BaseResponse;
import com.tup.reconac.feature.activo.dtos.request.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.response.ActivoResponse;
import com.tup.reconac.feature.activo.services.interfaces.IActivoCreateService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Activos")
@RestController
@RequestMapping("/api/activos")
@AllArgsConstructor
public class ActivoPostController {

    private final IActivoCreateService activoCreate;

    @Operation(summary = "Crear un activo", description = "Asocia el activo al escaneo indicado en el cuerpo.")
    @ApiResponse(responseCode = "201", description = "Creado", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
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
