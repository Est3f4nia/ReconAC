package com.tup.reconac.feature.usuario.services.domain;

import com.tup.reconac.exceptions.global.BadRequestException;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.repositories.UsuarioRepository;
import com.tup.reconac.feature.usuario.services.NvdApiKeyEncryptionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UsuarioKeyService {

    private final UsuarioRepository repo;
    private final UserDetailsService userService;
    private final NvdApiKeyEncryptionService encryptionService;

    public String getApiKey() {
        Usuario usuario = userService.getAuthenticatedUser();
        String encryptedKey = usuario.getNvdApiKey();

        if (encryptedKey == null || encryptedKey.isBlank()) {
            return null;
        }

        return encryptionService.decrypt(encryptedKey);
    }

    @Transactional
    public void updateNvdApiKey(String nvdApiKey) {
        Usuario usuario = userService.getAuthenticatedUser();

        if (nvdApiKey == null || nvdApiKey.isBlank()) {
            throw new BadRequestException(
                    "La NVD API key es obligatoria"
            );
        }

        String encryptedKey = encryptionService.encrypt(nvdApiKey);

        usuario.setNvdApiKey(encryptedKey);
        repo.save(usuario);
    }
}
