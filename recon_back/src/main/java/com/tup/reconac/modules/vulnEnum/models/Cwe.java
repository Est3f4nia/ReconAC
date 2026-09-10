package com.tup.reconac.modules.vulnEnum.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "cwe")
@RequiredArgsConstructor
@Getter
@Setter
public class Cwe implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cwe_id")
    private UUID id;

    @Column(name = "cwe_code", nullable = false, unique = true, length = 20)
    private String cweCode;

    @Column(name = "descripcion")
    private String descripcion;

}
