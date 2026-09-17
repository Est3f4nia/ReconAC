package com.tup.reconac.feature.auditoria.services.domain;

import com.tup.reconac.exceptions.auditoria.AuditoriaNotFoundException;
import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuditoriaConsultService {

    private final AuditoriaRepository repo;
    private final UserDetailsService userService;

    // Finders ---

    public Auditoria findId(UUID auditoriaId) {
        return repo.findById(auditoriaId)
                .orElseThrow(() -> new AuditoriaNotFoundException("Auditoría no encontrada: " + auditoriaId));
    }

    public List<Auditoria> findAllByUsuarioId(UUID usuarioId) {
        return repo.findByUsuarioIdOrderByFechaGeneracionDesc(usuarioId);
    }

    public Auditoria findOwnedAuditoria(
            UUID auditoriaId,
            UUID usuarioId
    ) {
        return repo.findByIdAndUsuarioId(
                auditoriaId,
                usuarioId
        ).orElseThrow(() ->
                new AuditoriaNotFoundException(
                        "Auditoría no encontrada"
                )
        );
    }

    // Verifiers ---

    public void verifyAuditoriaOwnership(UUID auditoriaId) {
        Auditoria auditoria = this.findId(auditoriaId);
        Usuario usuario = userService.getAuthenticatedUser();
        if (!auditoria.getUsuarioId().equals(usuario.getId())) {
            // notFound para evitar enumeración de auditorías
            throw new AuditoriaNotFoundException("Auditoría no encontrada"); // No pertenece al usuario
        }
    }

    // Por las relaciones entre tablas, la comprobación de Escaneo se hace desde acá
    public void verifyEscaneoOwnership(Escaneo escaneo, UUID usuarioId) {
        repo.findByIdAndUsuarioId(escaneo.getAuditoriaId(), usuarioId)
                .orElseThrow(() ->
                        new EscaneoNotFoundException("Escaneo no encontrado"));
    }

}
