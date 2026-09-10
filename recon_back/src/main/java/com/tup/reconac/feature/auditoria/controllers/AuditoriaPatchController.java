package com.tup.reconac.feature.auditoria.controllers;

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

@RestController
@RequestMapping("/api/auditorias")
@AllArgsConstructor
public class AuditoriaPatchController {

    private final IAuditoriaUpdateService auditoriaUpdate;

    @PatchMapping("/{id}")
    public ResponseEntity<BaseResponse<AuditoriaResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AuditoriaRequestDto req
    ) {
        AuditoriaResponse response = auditoriaUpdate.update(req, id);
        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.ok(response, "Auditoría actualizada correctamente")
        );
    }
}