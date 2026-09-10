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
@Table(name = "cve_cwe")
@NoArgsConstructor
@Getter
@Setter
public class CveCwe implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cve_cwe_id")
    private UUID id;

    @Column(name = "cve_id", nullable = false)
    private UUID cveId;

    @Column(name = "cwe_id", nullable = false)
    private UUID cweId;

    public CveCwe(UUID cveId, UUID cweId) {
        this.cveId = cveId;
        this.cweId = cweId;
    }
}
