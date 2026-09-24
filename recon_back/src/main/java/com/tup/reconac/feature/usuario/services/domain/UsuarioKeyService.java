package com.tup.reconac.feature.usuario.services.domain;

import com.tup.reconac.exceptions.usuario.KeyNotValidException;
import com.tup.reconac.exceptions.usuario.UserNotFoundException;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.repositories.UsuarioRepository;
import com.tup.reconac.feature.usuario.services.NvdApiKeyEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioKeyService {

    private final UsuarioRepository repo;
    private final CurrentUserService currentUser;
    private final NvdApiKeyEncryptionService encryptionService;

    public String getApiKey() {

        UUID usuarioId = currentUser.getUsuarioId();
        Usuario usuario = repo.findById(usuarioId).orElseThrow(() ->
                new UserNotFoundException("Usuario no encontrado"));

        String encryptedKey = usuario.getNvdApiKey();

        if (encryptedKey == null || encryptedKey.isBlank()) return null;
        return encryptionService.decrypt(encryptedKey);
    }

    public void updateKey(Usuario usuario, String apiKey) {

        if (apiKey == null) {
            throw new KeyNotValidException("La NVD API key no puede estar vacía");
        }

        String normalized = apiKey.strip();

        if (normalized.isEmpty()) {
            throw new KeyNotValidException("La NVD API key no puede estar vacía");
        }

        usuario.setNvdApiKey(encryptionService.encrypt(normalized));
    }

    @Transactional
    public void updateNvdApiKey(String nvdApiKey) {

        UUID usuarioId = currentUser.getUsuarioId();
        Usuario usuario = repo.findById(usuarioId).orElseThrow(() ->
                new UserNotFoundException("Usuario no encontrado"));

        updateKey(usuario, nvdApiKey);
    }
}
