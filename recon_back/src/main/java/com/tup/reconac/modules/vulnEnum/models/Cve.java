package com.tup.reconac.modules.vulnEnum.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cve")
@RequiredArgsConstructor
@Getter
public class Cve implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cve_id")
    private UUID id;

    @Column(name = "cve", nullable = false, unique = true, length = 16)
    private String cve;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "severidad")
    private String severidad;

    @Column(name = "cvss", precision = 3, scale = 1)
    private BigDecimal cvss;

    @Column(name = "vector_cvss")
    private String vectorCvss;

    @Column(name = "epss", precision = 5, scale = 4)
    private BigDecimal epss;

    @Column(name = "kev")
    private Boolean kev;

    @Column(name = "version_vuln")
    private String versionVuln;

    @Column(name = "mitigacion")
    private String mitigacion;

    @Column(name = "version_parche")
    private String versionParche;

    @Column(name = "tipo_parche")
    private String tipoParche;

    @Column(name = "exploit_refs")
    private String exploitRefs;

    @Column(name = "url_nist")
    private String urlNist;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @Column(name = "ult_modificacion")
    private LocalDateTime ultModificacion;

}
