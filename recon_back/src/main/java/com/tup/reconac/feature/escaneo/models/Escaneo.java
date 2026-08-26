package com.tup.reconac.feature.escaneo.models;

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
@Table(name = "escaneo")
@NoArgsConstructor
@Getter
@Setter
public class Escaneo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "escaneo_id")
    private UUID id;

    @Column(name = "auditoria_id", nullable = false)
    private UUID auditoriaId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "objetivos", columnDefinition = "text[]")
    private String[] objetivos;

    @Column(name = "estado", nullable = false)
    private String estado = "QUEUED";

    @Column(name = "progreso", nullable = false)
    private Integer progreso = 0;

    @Column(name = "modulo_job_id")
    private String moduloJobId;

    @Column(name = "nmap_version")
    private String nmapVersion;

    @Column(name = "mensaje_error")
    private String mensajeError;

    @Column(name = "iniciado_a")
    private LocalDateTime iniciadoA;

    @Column(name = "completado_a")
    private LocalDateTime completadoA;

    @Column(name = "creado_a", nullable = false)
    private LocalDateTime creadoA;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resultado")
    private String resultado;

    @PrePersist
    public void prePersist() {
        if (this.creadoA == null) {
            this.creadoA = LocalDateTime.now();
        }
    }
}
