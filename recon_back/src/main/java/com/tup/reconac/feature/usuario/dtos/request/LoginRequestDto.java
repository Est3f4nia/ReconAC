package com.tup.reconac.feature.usuario.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank(message = "El email es obligatorio")
        @Email
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasenia
) {}
