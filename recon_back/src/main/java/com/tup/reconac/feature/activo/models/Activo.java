package com.tup.reconac.feature.activo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "activo")
@NoArgsConstructor
@Getter
@Setter
public class Activo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "activo_id")
    private UUID id;

    @Column(name = "auditoria_id", nullable = false)
    private UUID auditoriaId;

    // string interpretado como inet en db (Hibernate)
    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "host", nullable = false)
    private String host;

    @Column(name = "hostname")
    private String hostname;

    @Column(name = "so")
    private String so;

    @Column(name = "so_probab")
    private Integer soProbab;

    @JdbcTypeCode(SqlTypes.OTHER)
    @Column(name = "mac")
    private String mac;

    @Column(name = "descripcion")
    private String descripcion;
}
