package com.tup.reconac.feature.auditoria.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.auditoria.dtos.request.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaUpdateService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Auditorías")
@RestController
@RequestMapping("/api/auditorias")
@AllArgsConstructor
public class AuditoriaPatchController {

    private final IAuditoriaUpdateService auditoriaUpdate;

    @Operation(summary = "Actualizar una auditoría", description = "Actualiza nombre y objetivo con las validaciones del DTO existente.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @PatchMapping("/{auditoriaId}")
    public ResponseEntity<BaseResponse<AuditoriaResponse>> update(
            @PathVariable UUID auditoriaId,
            @Valid @RequestBody AuditoriaRequestDto req
    ) {
        AuditoriaResponse response = auditoriaUpdate.update(req, auditoriaId);
        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.ok(response, "Auditoría actualizada correctamente")
        );
    }
}