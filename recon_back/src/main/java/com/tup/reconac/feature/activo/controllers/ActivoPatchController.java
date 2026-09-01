package com.tup.reconac.feature.activo.controllers;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.activo.dtos.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.ActivoResponse;
import com.tup.reconac.feature.activo.services.interfaces.IActivoUpdateService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/activos")
@AllArgsConstructor
public class ActivoPatchController {

    private final IActivoUpdateService activoUpdate;

    @PatchMapping("/{id}")
    public ResponseEntity<BaseResponse<ActivoResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ActivoRequestDto req
    ) {
        ActivoResponse response = activoUpdate.update(req, id);
        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.ok(response, "Activo actualizado correctamente")
        );
    }
}
