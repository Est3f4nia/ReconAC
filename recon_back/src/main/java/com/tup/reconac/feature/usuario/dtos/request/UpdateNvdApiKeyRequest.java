package com.tup.reconac.feature.usuario.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateNvdApiKeyRequest(

        @NotBlank(message = "La API KEY es obligatoria")
        String apiKey

) {
}
