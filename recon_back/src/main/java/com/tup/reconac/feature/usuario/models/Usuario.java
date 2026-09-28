package com.tup.reconac.feature.usuario.models;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "usuario")
@NoArgsConstructor
public class Usuario implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "usuario_id")
    @Getter
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 50)
    @Getter
    @Setter
    private String email;

    @Column(name = "contrasenia", nullable = false)
    @Getter
    @Setter
    private String contrasenia;

    @Column(name = "nvd_api_key")
    @Getter
    @Setter
    private String nvdApiKey;

}
