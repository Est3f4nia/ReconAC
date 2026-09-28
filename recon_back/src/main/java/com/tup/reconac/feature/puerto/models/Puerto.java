package com.tup.reconac.feature.puerto.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "puerto")
@NoArgsConstructor
@Getter
@Setter
public class Puerto implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "puerto_id")
    private UUID id;

    @Column(name = "activo_id", nullable = false)
    private UUID activoId;

    @Column(name = "servicio_fallback")
    private String servicioFallback;

    @Column(name = "producto")
    private String producto;

    @Column(name = "version")
    private String version;

    @Column(name = "extra_info")
    private String extraInfo;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "protocolo", nullable = false)
    private String protocolo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
            name = "estado",
            columnDefinition = "estado_puerto_enum",
            nullable = false
    )
    private PuertoEstado estado;
}