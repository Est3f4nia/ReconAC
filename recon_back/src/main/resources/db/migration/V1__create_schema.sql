-- ============================================================
-- ReconAC - Initial database schema
-- Flyway migration V1
-- PostgreSQL
-- ============================================================

-- ===== ENUM TYPES =====

CREATE TYPE estado_puerto_enum AS ENUM (
    'open',
    'closed',
    'filtered'
);

-- ===== USUARIO =====

CREATE TABLE Usuario (
    usuario_id UUID PRIMARY KEY,
    email VARCHAR(50) NOT NULL UNIQUE,
    contrasenia VARCHAR(70) NOT NULL
);

-- ===== AUDITORIA =====

CREATE TABLE Auditoria (
    auditoria_id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,

    nombre VARCHAR(50),
    objetivo VARCHAR(150),

    fecha_generacion TIMESTAMP NOT NULL,
    fecha_final TIMESTAMP,

    CONSTRAINT fk_auditoria_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES Usuario(usuario_id)
);

-- ===== ACTIVO =====

CREATE TABLE Activo (
    activo_id UUID PRIMARY KEY,

    host INET NOT NULL,
    hostname TEXT,
    so TEXT,
    so_probab INTEGER,

    CONSTRAINT chk_activo_so_probab
        CHECK (so_probab BETWEEN 0 AND 100),
    mac MACADDR,
    descripcion TEXT
);

-- ===== CPE =====

CREATE TABLE Cpe (
    cpe_id UUID PRIMARY KEY,

    uri VARCHAR(170) NOT NULL UNIQUE,
    uri_legible TEXT NOT NULL,
    servicio_nombre TEXT,
    vendor TEXT,
    producto TEXT,
    version TEXT,
    ultimo_check TIMESTAMP NOT NULL DEFAULT now()
);

-- ===== CVE =====

CREATE TABLE Cve (
    cve_id UUID PRIMARY KEY,

    cve VARCHAR(16) NOT NULL UNIQUE,
    descripcion TEXT,
    severidad TEXT,

    cvss DECIMAL(3,1),
    vector_cvss TEXT,
    epss DECIMAL(5,4),
    kev BOOLEAN,

    version_vuln TEXT,
    mitigacion TEXT,
    version_parche TEXT,
    tipo_parche TEXT,
    exploit_refs TEXT,
    url_nist TEXT,

    fecha_publicacion TIMESTAMP,
    ult_modificacion TIMESTAMP,

    CONSTRAINT chk_cve_cvss
        CHECK (cvss BETWEEN 0.0 AND 10.0),

    CONSTRAINT chk_cve_epss
        CHECK (epss BETWEEN 0.0000 AND 1.0000)
);

-- ===== PUERTO =====

CREATE TABLE Puerto (
    puerto_id UUID PRIMARY KEY,
    activo_id UUID NOT NULL,

    servicio_fallback TEXT,
    numero INTEGER NOT NULL,
    protocolo VARCHAR(10) NOT NULL,
    estado estado_puerto_enum NOT NULL,

    CONSTRAINT chk_puerto_numero
        CHECK (numero BETWEEN 1 AND 65535),

    CONSTRAINT fk_puerto_activo
        FOREIGN KEY (activo_id)
            REFERENCES Activo(activo_id),

    CONSTRAINT uq_puerto_activo_numero_protocolo_estado
        UNIQUE (activo_id, numero, protocolo, estado)
);

-- ===== PUERTO_CPE (M:N) =====

CREATE TABLE Puerto_Cpe (
    puerto_cpe_id UUID PRIMARY KEY,

    puerto_id UUID NOT NULL,
    cpe_id UUID NOT NULL,

    CONSTRAINT fk_puerto_cpe_puerto
        FOREIGN KEY (puerto_id)
            REFERENCES Puerto(puerto_id),

    CONSTRAINT fk_puerto_cpe_cpe
        FOREIGN KEY (cpe_id)
            REFERENCES Cpe(cpe_id),

    CONSTRAINT uq_puerto_cpe
        UNIQUE (puerto_id, cpe_id)
);

-- ===== CPE_CVE (M:N) =====

CREATE TABLE Cpe_Cve (
    cpe_cve_id UUID PRIMARY KEY,

    cpe_id UUID NOT NULL,
    cve_id UUID NOT NULL,

    CONSTRAINT fk_cpe_cve_cpe
        FOREIGN KEY (cpe_id)
            REFERENCES Cpe(cpe_id),

    CONSTRAINT fk_cpe_cve_cve
        FOREIGN KEY (cve_id)
            REFERENCES Cve(cve_id),

    CONSTRAINT uq_cpe_cve
        UNIQUE (cpe_id, cve_id)
);

-- ===== REFERENCIA =====

CREATE TABLE Referencia (
    referencia_id UUID PRIMARY KEY,
    cve_id UUID NOT NULL,

    url TEXT NOT NULL,
    source TEXT,
    tags TEXT[],

    CONSTRAINT fk_referencia_cve
        FOREIGN KEY (cve_id)
            REFERENCES Cve(cve_id)
);

-- ===== CWE =====

CREATE TABLE Cwe (
    cwe_id UUID PRIMARY KEY,

    cwe_code VARCHAR(20) NOT NULL UNIQUE,
    descripcion TEXT
);

-- ===== CVE_CWE (M:N) =====

CREATE TABLE Cve_Cwe (
    cve_cwe_id UUID PRIMARY KEY,

    cve_id UUID NOT NULL,
    cwe_id UUID NOT NULL,

    CONSTRAINT fk_cve_cwe_cve
        FOREIGN KEY (cve_id)
            REFERENCES Cve(cve_id),

    CONSTRAINT fk_cve_cwe_cwe
        FOREIGN KEY (cwe_id)
            REFERENCES Cwe(cwe_id),

    CONSTRAINT uq_cve_cwe
        UNIQUE (cve_id, cwe_id)
);


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_auditoria_usuario_id
    ON Auditoria(usuario_id);

CREATE INDEX idx_activo_host
    ON Activo(host);

CREATE INDEX idx_puerto_activo_id
    ON Puerto(activo_id);

CREATE INDEX idx_puerto_estado
    ON Puerto(estado);

CREATE INDEX idx_puerto_cpe_puerto_id
    ON Puerto_Cpe(puerto_id);

CREATE INDEX idx_puerto_cpe_cpe_id
    ON Puerto_Cpe(cpe_id);

CREATE INDEX idx_cpe_uri
    ON Cpe(uri);

CREATE INDEX idx_cpe_cve_cpe_id
    ON Cpe_Cve(cpe_id);

CREATE INDEX idx_cpe_cve_cve_id
    ON Cpe_Cve(cve_id);

CREATE INDEX idx_cve_cve
    ON Cve(cve);

CREATE INDEX idx_cve_severidad
    ON Cve(severidad);

CREATE INDEX idx_cve_cvss
    ON Cve(cvss);

CREATE INDEX idx_referencia_cve_id
    ON Referencia(cve_id);

CREATE INDEX idx_cve_cwe_cve_id
    ON Cve_Cwe(cve_id);

CREATE INDEX idx_cve_cwe_cwe_id
    ON Cve_Cwe(cwe_id);

CREATE INDEX idx_cwe_cwe_code
    ON Cwe(cwe_code);
