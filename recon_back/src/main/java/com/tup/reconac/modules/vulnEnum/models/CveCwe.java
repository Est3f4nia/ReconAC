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
@Table(name = "cve_cwe")
@RequiredArgsConstructor
@Getter
public class CveCwe implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cve_cwe_id")
    private UUID id;

    @Column(name = "cve_id", nullable = false)
    private UUID cveId;

    @Column(name = "cwe_id", nullable = false)
    private UUID cweId;
}
