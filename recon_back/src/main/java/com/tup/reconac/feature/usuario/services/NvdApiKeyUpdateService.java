package com.tup.reconac.feature.usuario.services;

import com.tup.reconac.exceptions.usuario.UserNotFoundException;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.repositories.UsuarioRepository;
import com.tup.reconac.feature.usuario.services.domain.CurrentUserService;
import com.tup.reconac.feature.usuario.services.domain.UsuarioKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NvdApiKeyUpdateService {

    private final CurrentUserService currentUser;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioKeyService usuarioKeyService;

    @Transactional
    public void update(String apiKey) {

        UUID usuarioId = currentUser.getUsuarioId();
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(() ->
                new UserNotFoundException("Usuario no encontrado"));

        usuarioKeyService.updateKey(usuario, apiKey);
    }
}
