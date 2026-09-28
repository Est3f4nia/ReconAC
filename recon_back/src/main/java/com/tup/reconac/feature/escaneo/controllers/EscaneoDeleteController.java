package com.tup.reconac.feature.escaneo.controllers;

import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoDeleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Escaneos")
@RestController
@RequestMapping("/api/auditorias/{auditoriaId}/escaneos")
@RequiredArgsConstructor
public class EscaneoDeleteController {

    private final IEscaneoDeleteService escaneoDelete;

    @Operation(summary = "Eliminar un escaneo", description = "El servicio resuelve la propiedad por escaneoId; auditoriaId forma parte de la ruta pero no se utiliza para resolver la eliminación.")
    @ApiResponse(responseCode = "204", description = "Eliminado; sin cuerpo", content = @io.swagger.v3.oas.annotations.media.Content)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @io.swagger.v3.oas.annotations.Parameter(name = "auditoriaId", in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH, required = true, description = "UUID de la ruta; el servicio actual resuelve por escaneoId", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "uuid"))
    @DeleteMapping("/{escaneoId}")
    public ResponseEntity<BaseResponse<Void>> deleteEscaneo(
            @PathVariable UUID escaneoId
    ) {
        escaneoDelete.eliminar(escaneoId);

        return ResponseEntity.noContent().build();
    }
}
