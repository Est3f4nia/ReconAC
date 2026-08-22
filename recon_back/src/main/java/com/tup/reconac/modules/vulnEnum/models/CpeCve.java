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
@Table(name = "cpe_cve")
@RequiredArgsConstructor
@Getter
public class CpeCve implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cpe_cve_id")
    private UUID id;

    @Column(name = "cpe_id", nullable = false)
    private UUID cpeId;

    @Column(name = "cve_id", nullable = false)
    private UUID cveId;
}
