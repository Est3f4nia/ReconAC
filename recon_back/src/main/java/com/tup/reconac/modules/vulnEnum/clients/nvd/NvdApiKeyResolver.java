package com.tup.reconac.modules.vulnEnum.clients.nvd;

import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.repositories.UsuarioRepository;
import com.tup.reconac.feature.usuario.services.NvdApiKeyEncryptionService;
import com.tup.reconac.modules.vulnEnum.dtos.enrichment.nvd.NvdLookupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NvdApiKeyResolver {

    private final EscaneoRepository escaneoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NvdApiKeyEncryptionService encryptionService;

    public String resolve(
            NvdLookupRequest request
    ) {

        if (request.escaneoId() == null) {
            throw new IllegalArgumentException(
                    "escaneoId es obligatorio para consultar NVD"
            );
        }

        Escaneo escaneo =
                escaneoRepository
                        .findById(request.escaneoId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Escaneo no encontrado: "
                                                + request.escaneoId()
                                )
                        );

        Auditoria auditoria =
                auditoriaRepository
                        .findById(
                                escaneo.getAuditoriaId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Auditoría asociada al escaneo no encontrada"
                                )
                        );

        Usuario usuario =
                usuarioRepository
                        .findById(
                                auditoria.getUsuarioId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Usuario asociado a la auditoría no encontrado"
                                )
                        );

        String encrypted =
                usuario.getNvdApiKey();

        if (encrypted == null
                || encrypted.isBlank()) {

            throw new IllegalStateException(
                    "El usuario no tiene una NVD API key configurada"
            );
        }

        return encryptionService.decrypt(
                encrypted
        );
    }
}
