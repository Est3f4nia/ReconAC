package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.exceptions.global.BadRequestException;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoPatchService;
import com.tup.reconac.feature.usuario.services.domain.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EscaneoPatchService implements IEscaneoPatchService {

    private final EscaneoConsultService escaneoConsult;
    private final AuditoriaConsultService auditoriaConsult;
    private final CurrentUserService currentUser;

    @Override
    @Transactional
    public void mover(UUID escaneoId, UUID nuevaAuditoriaId) {

        UUID usuarioId = currentUser.getUsuarioId();
        Escaneo escaneo = escaneoConsult.findEscaneoForUsuario(escaneoId);

        if (escaneo.getEstado() == EscaneoEstado.EN_PROCESO) {
            throw new BadRequestException("No se puede mover un escaneo en proceso");
        }

        if (escaneo.getAuditoriaId().equals(nuevaAuditoriaId)) return;

        auditoriaConsult.findOwnedAuditoria(nuevaAuditoriaId, usuarioId);
        escaneo.setAuditoriaId(nuevaAuditoriaId);
    }
}
