package com.tup.reconac.feature.activo.services;

import com.tup.reconac.feature.activo.dtos.request.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.response.ActivoResponse;
import com.tup.reconac.feature.activo.mappers.ActivoMapper;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.activo.services.interfaces.IActivoCreateService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


// el módulo consulta este endpoint?
@Service
@AllArgsConstructor
public class ActivoCreateService implements IActivoCreateService {

    private final ActivoRepository repo;

    @Override
    @Transactional
    public ActivoResponse create(ActivoRequestDto req) {

//        Usuario usuario = userService.getAuthenticatedUser();
//
//        auditoriaConsult.verifyEscaneoOwnership(escaneo, usuario.getId());
//        if (repo.findByHostnameAndHost()){
//
//        }
//        Falta validar usuario, activo repetido, y comprobar ownership
//
        Activo activo = ActivoMapper.toEntity(req);
        Activo saved = repo.save(activo);
        return ActivoMapper.toResponse(saved);
    }
}
