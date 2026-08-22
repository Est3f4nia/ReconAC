package com.tup.reconac.feature.auditoria.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auditoria")
@NoArgsConstructor
@Getter
@Setter
public class Auditoria implements Serializable { // que chota es eso

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "auditoria_id")
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "nombre", length = 50)
    private String nombre;

    @Column(name = "objetivo", length = 150)
    private String objetivo;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "fecha_final")
    private LocalDateTime fechaFinal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resultado_jsonb", nullable = false)
    private String resultadoJsonb;

    @Column(name = "nmap_version")
    private String nmapVersion;

    @PrePersist
    public void prePersist() {
        this.fechaGeneracion = LocalDateTime.now();
    }
}
