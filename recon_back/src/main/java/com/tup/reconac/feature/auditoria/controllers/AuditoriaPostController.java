package com.tup.reconac.feature.auditoria.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.auditoria.dtos.request.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaCreateService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auditorías")
@RestController
@RequestMapping("/api/auditorias")
@AllArgsConstructor
public class AuditoriaPostController {

    private final IAuditoriaCreateService auditoriaCreate;

    @Operation(summary = "Crear una auditoría", description = "Crea una auditoría para el usuario autenticado.")
    @ApiResponse(responseCode = "201", description = "Creado", useReturnTypeSchema = true)
    @PostMapping
    public ResponseEntity<BaseResponse<AuditoriaResponse>> create(
            @Valid @RequestBody AuditoriaRequestDto dto
    ) {
        AuditoriaResponse response = auditoriaCreate.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.ok(response, "Auditoría creada correctamente")
        );
    }
}
