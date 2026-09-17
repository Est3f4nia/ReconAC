package com.tup.reconac.feature.usuario.services;

import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.repositories.UsuarioRepository;
import com.tup.reconac.feature.usuario.services.domain.UserDetailsService;
import com.tup.reconac.feature.usuario.services.domain.UsuarioKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NvdApiKeyUpdateService {
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioKeyService usuarioKeyService;

    @Transactional
    public void update(String apiKey) {

        String normalized =
                apiKey == null
                        ? ""
                        : apiKey.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "La API KEY no puede estar vacía"
            );
        }

        Usuario usuario =
                userDetailsService.getAuthenticatedUser();

        usuarioKeyService.updateKey(
                usuario,
                normalized
        );

        usuarioRepository.save(usuario);
    }
}
