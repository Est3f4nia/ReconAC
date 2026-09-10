package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.exceptions.global.BadRequestException;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoPatchService;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class EscaneoPatchService implements IEscaneoPatchService {

    private final AuditoriaConsultService auditoriaConsult;
    private final UserDetailsService userService;
    private final EscaneoRepository repo;

    @Override
    @Transactional
    public void mover(UUID escaneoId, UUID nuevaAuditoriaId) {

        Usuario usuario = userService.getAuthenticatedUser();

        Escaneo escaneo = repo.findById(escaneoId)
                .orElseThrow(() ->
                        new EscaneoNotFoundException("Escaneo no encontrado (patch)")
                );

        auditoriaConsult.verifyEscaneoOwnership(escaneo, usuario.getId());
        Auditoria nuevaAuditoria = auditoriaConsult.findId(nuevaAuditoriaId);

        if (!nuevaAuditoria.getUsuarioId().equals(usuario.getId())) {
            throw new BadRequestException(
                    "La auditoría no pertenece al usuario autenticado"
            );
        }

        if (escaneo.getEstado() == EscaneoEstado.EN_PROCESO) {
            throw new BadRequestException(
                    "No se puede mover un escaneo en proceso"
            );
        }

        escaneo.setAuditoriaId(nuevaAuditoriaId);
        repo.save(escaneo);
    }
}
