package com.tup.reconac.feature.auditoria.controllers;

import com.tup.reconac.feature.auditoria.dtos.AuditoriaResponse;

import java.util.List;

import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditorias")
@RequiredArgsConstructor
public class AuditoriaGetController {

    private final IAuditoriaGetService auditoriaGet;

    @GetMapping("/all")
    public ResponseEntity<List<AuditoriaResponse>> findAll() {
        return ResponseEntity.ok(auditoriaGet.getAll());
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<BaseResponse<AuditoriaResponse>> findById(@PathVariable UUID id) {
//        return auditoriaService.findById(id)
//                .map(a -> ResponseEntity.ok(BaseResponse.ok(AuditoriaMapper.toResponse(a))))
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    // ver esto, hace falta?
//    @GetMapping("/usuario/{usuarioId}")
//    public ResponseEntity<BaseResponse<Page<AuditoriaResponse>>> findByUsuarioId(
//            @PathVariable UUID usuarioId, Pageable pageable) {
//        Page<AuditoriaResponse> page = auditoriaService.findByUsuarioId(usuarioId, pageable)
//                .map(AuditoriaMapper::toResponse);
//        return ResponseEntity.ok(BaseResponse.ok(page));
//    }
}
