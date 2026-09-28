-- ============================================================
-- ReconAC - Add table Escaneo + API KEY to each user
-- Flyway migration V2
-- PostgreSQL
-- ============================================================

-- ===== ENUM TYPES =====

CREATE TYPE estado_escaneo_enum AS ENUM (
    'PENDIENTE',
    'EN_PROCESO',
    'COMPLETADO',
    'FALLO'
);

-- ===== ESCANEO =====

CREATE TABLE Escaneo (
    escaneo_id UUID PRIMARY KEY,
    auditoria_id UUID NOT NULL,

    objetivos TEXT[] NOT NULL,
    estado estado_escaneo_enum NOT NULL DEFAULT 'PENDIENTE',
    progreso INTEGER NOT NULL DEFAULT 0,

    modulo_job_id VARCHAR(100),
    nmap_version TEXT,
    mensaje_error TEXT,

    iniciado_a TIMESTAMP,
    completado_a TIMESTAMP,
    creado_a TIMESTAMP NOT NULL DEFAULT now(),

    resultado JSONB,

    CONSTRAINT fk_escaneo_auditoria
        FOREIGN KEY (auditoria_id)
            REFERENCES Auditoria(auditoria_id),

    CONSTRAINT chk_escaneo_progreso
        CHECK (progreso BETWEEN 0 AND 100)
);

-- ===== ACTIVO =====

ALTER TABLE Activo
    ADD COLUMN escaneo_id UUID NOT NULL,
    ADD CONSTRAINT fk_activo_escaneo
        FOREIGN KEY (escaneo_id)
        REFERENCES Escaneo(escaneo_id);

-- ===== USUARIO =====

ALTER TABLE Usuario
    ADD COLUMN nvd_api_key VARCHAR(100) UNIQUE;


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_escaneo_auditoria_id
    ON Escaneo(auditoria_id);

CREATE INDEX idx_escaneo_estado
    ON Escaneo(estado);

CREATE INDEX idx_activo_escaneo_id
    ON Activo(escaneo_id);