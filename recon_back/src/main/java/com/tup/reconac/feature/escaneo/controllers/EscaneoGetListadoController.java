package com.tup.reconac.feature.escaneo.controllers;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.escaneo.dtos.EscaneoListadoResponse;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoGetService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/escaneos")
@RequiredArgsConstructor
public class EscaneoGetListadoController {

    private final EscaneoConsultService escaneoGet;

    @GetMapping
    public ResponseEntity<BaseResponse<Page<EscaneoListadoResponse>>> findAll(
            Pageable pageable) {

        Page<EscaneoListadoResponse> response =
                escaneoGet.findAllForAuthenticatedUser(pageable);

        return ResponseEntity.ok(
                BaseResponse.ok(response, "Escaneos obtenidos")
        );
    }
}
