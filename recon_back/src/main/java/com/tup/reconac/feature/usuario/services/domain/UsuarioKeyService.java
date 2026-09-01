package com.tup.reconac.feature.usuario.services.domain;

import com.tup.reconac.exceptions.usuario.KeyNotValidException;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.repositories.UsuarioRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UsuarioKeyService {

    private final UsuarioRepository repo;
    private final UserDetailsService userService;

    @Transactional
    public void updateNvdApiKey(String nvdApiKey) {
        Usuario usuario = userService.getAuthenticatedUser();

        if (nvdApiKey == null || nvdApiKey.isBlank()) {
            usuario.setNvdApiKeyHash(null);
            repo.save(usuario);
            return;
        }

        String hash = hash(nvdApiKey);
        Optional<Usuario> existing = repo.findByNvdApiKeyHash(hash);
        if (existing.isPresent() && !existing.get().getId().equals(usuario.getId())) {
            throw new KeyNotValidException("La API key proporcionada no es válida");
        }

        usuario.setNvdApiKeyHash(hash);
        repo.save(usuario);
    }

    public boolean isUserKey(Usuario usuario, String rawKey) {
        if (rawKey == null || rawKey.isBlank() || usuario.getNvdApiKeyHash() == null) {
            return false;
        }
        return hash(rawKey).equals(usuario.getNvdApiKeyHash());
    }

    private String hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
