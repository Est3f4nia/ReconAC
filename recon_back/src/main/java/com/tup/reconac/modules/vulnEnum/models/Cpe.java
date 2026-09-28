package com.tup.reconac.modules.vulnEnum.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cpe")
@NoArgsConstructor
// (access = AccessLevel.PROTECTED)
@Getter
@Setter
public class Cpe implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cpe_id")
    private UUID id;

    @Column(name = "uri", nullable = false, unique = true, length = 170)
    private String uri;

    @Column(name = "uri_legible", nullable = false)
    private String uriLegible;

    @Column(name = "servicio_nombre")
    private String servicioNombre;

    @Column(name = "vendor")
    private String vendor;

    @Column(name = "producto")
    private String producto;

    @Column(name = "version")
    private String version;

    @Column(name = "ultimo_check")
    private LocalDateTime ultimoCheck;

}

