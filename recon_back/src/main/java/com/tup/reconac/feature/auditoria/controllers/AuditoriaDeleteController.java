package com.tup.reconac.feature.auditoria.controllers;

import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaDeleteService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auditorias")
@AllArgsConstructor
public class AuditoriaDeleteController {

    private IAuditoriaDeleteService auditoriaDelete;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        auditoriaDelete.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
