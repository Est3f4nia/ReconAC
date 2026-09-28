package com.tup.reconac.feature.activo.services;

import com.tup.reconac.exceptions.activo.ActivoNotFoundException;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.activo.services.interfaces.IActivoDeleteService;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.usuario.services.domain.CurrentUserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ActivoDeleteService implements IActivoDeleteService {

    private final ActivoRepository repo;
    private final AuditoriaConsultService auditoriaConsult;
    private final EscaneoConsultService escaneoConsult;
    private final CurrentUserService currentUser;

    @Override
    @Transactional
    public void deleteById(UUID activoId) {

        UUID usuarioId = currentUser.getUsuarioId();

        Activo activo = repo.findById(activoId)
                .orElseThrow(() ->
                        new ActivoNotFoundException("El activo no existe")
                );

        Escaneo escaneo = escaneoConsult.findById(activo.getEscaneoId());
        auditoriaConsult.verifyEscaneoOwnership(escaneo, usuarioId);

        repo.delete(activo);
    }
}
