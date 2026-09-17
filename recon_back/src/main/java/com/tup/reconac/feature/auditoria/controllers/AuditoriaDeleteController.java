package com.tup.reconac.feature.auditoria.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.server.BaseResponse;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaDeleteService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Auditorías")
@RestController
@RequestMapping("/api/auditorias")
@AllArgsConstructor
public class AuditoriaDeleteController {

    private IAuditoriaDeleteService auditoriaDelete;

    @Operation(summary = "Eliminar una auditoría", description = "Elimina la auditoría y sus datos asociados según el servicio actual.")
    @ApiResponse(responseCode = "204", description = "Eliminado; sin cuerpo", content = @io.swagger.v3.oas.annotations.media.Content)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @DeleteMapping("/{auditoriaId}")
    public ResponseEntity<BaseResponse<Void>> deleteById(
            @PathVariable UUID auditoriaId
    ) {
        auditoriaDelete.deleteById(auditoriaId);
        return ResponseEntity.noContent().build();
    }
}
