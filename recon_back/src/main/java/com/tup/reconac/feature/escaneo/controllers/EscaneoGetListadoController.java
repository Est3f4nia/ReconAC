package com.tup.reconac.feature.escaneo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import com.tup.reconac.config.server.BaseResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoListadoResponse;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Escaneos")
@RestController
@RequestMapping("/api/escaneos")
@RequiredArgsConstructor
public class EscaneoGetListadoController {

    private final EscaneoConsultService escaneoGet;

    @Operation(summary = "Listar escaneos del usuario", description = "Devuelve BaseResponse con una Page de escaneos de las auditorías del usuario.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @GetMapping
    public ResponseEntity<BaseResponse<Page<EscaneoListadoResponse>>> findAll(
            @ParameterObject Pageable pageable) {

        Page<EscaneoListadoResponse> response =
                escaneoGet.findAllForAuthenticatedUser(pageable);

        return ResponseEntity.ok(
                BaseResponse.ok(response, "Escaneos obtenidos")
        );
    }
}
