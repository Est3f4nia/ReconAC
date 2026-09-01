package com.tup.reconac.feature.usuario.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank(message = "El email es obligatorio")
        @Size(max = 50)
        @Email
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 70, message = "La contraseña debe tener entre 8 y 70 caracteres")
        String contrasenia
) {}
