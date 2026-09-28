package com.tup.reconac.modules.vulnEnum.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Hook con la implementación global
 */

@Entity
@Table(name = "puerto_cpe")
@NoArgsConstructor
@Getter
@Setter
public class PuertoCpe implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "puerto_cpe_id")
    private UUID id;

    @Column(name = "puerto_id", nullable = false)
    private UUID puertoId;

    @Column(name = "cpe_id", nullable = false)
    private UUID cpeId;
}
