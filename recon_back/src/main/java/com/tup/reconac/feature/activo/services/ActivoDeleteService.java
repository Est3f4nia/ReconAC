package com.tup.reconac.feature.activo.services;

import com.tup.reconac.exceptions.activo.ActivoNotFoundException;
import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.activo.services.interfaces.IActivoDeleteService;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ActivoDeleteService implements IActivoDeleteService {

    private final ActivoRepository repo;
    private final EscaneoRepository escaneoRepository;
    private final AuditoriaConsultService auditoriaConsult;
    private final UserDetailsService userService;

    @Override
    @Transactional
    public void deleteById(UUID id) {

        Usuario usuario = userService.getAuthenticatedUser();

        Activo activo = repo.findById(id)
                .orElseThrow(() ->
                        new ActivoNotFoundException(
                                "El activo no existe"
                        )
                );

        Escaneo escaneo = escaneoRepository.findById(
                activo.getEscaneoId()
        ).orElseThrow(() ->
                new EscaneoNotFoundException(
                        "Escaneo asociado al activo no encontrado"
                )
        );

        auditoriaConsult.verifyEscaneoOwnership(
                escaneo,
                usuario.getId()
        );

        repo.delete(activo);
    }
}
