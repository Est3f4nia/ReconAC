package com.tup.reconac.modules.vulnEnum.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "referencia")
@NoArgsConstructor
@Getter
@Setter
public class Referencia implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "referencia_id")
    private UUID id;

    @Column(name = "cve_id", nullable = false)
    private UUID cveId;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "source")
    private String source;

    @Column(name = "tags")
    private String[] tags;

    public Referencia(UUID cveId, String url, String source, String[] tags) {
        this.cveId = cveId;
        this.url = url;
        this.source = source;
        this.tags = tags;
    }
}
