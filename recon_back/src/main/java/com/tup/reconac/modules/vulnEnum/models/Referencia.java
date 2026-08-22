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
import java.util.UUID;

@Entity
@Table(name = "referencia")
@RequiredArgsConstructor
@Getter
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
}
