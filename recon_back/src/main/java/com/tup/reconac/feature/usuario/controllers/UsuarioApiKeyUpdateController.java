package com.tup.reconac.feature.usuario.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.server.BaseResponse;
import com.tup.reconac.feature.usuario.dtos.request.UpdateNvdApiKeyRequest;
import com.tup.reconac.feature.usuario.services.NvdApiKeyUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth / Usuario")
@RestController
@RequestMapping("/api/usuarios/me")
@RequiredArgsConstructor
public class UsuarioApiKeyUpdateController {

    private final NvdApiKeyUpdateService apiKeyUpdateService;

    @Operation(summary = "Actualizar la clave NVD del usuario", description = "Actualiza la clave del usuario autenticado; no se devuelve la clave en la respuesta.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Error409")
    @PatchMapping("/nvd-api-key")
    public ResponseEntity<BaseResponse<Void>> updateNvdApiKey(
            @Valid @RequestBody UpdateNvdApiKeyRequest request
    ) {

        apiKeyUpdateService.update(
                request.apiKey()
        );

        return ResponseEntity.ok(
                BaseResponse.ok(
                        null,
                        "API KEY actualizada correctamente"
                )
        );
    }
}