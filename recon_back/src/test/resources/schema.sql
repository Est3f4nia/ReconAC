-- ============================================================
-- ReconAC - H2-compatible schema for tests
-- Mirrors V1 PostgreSQL migration
-- ============================================================

-- ===== USUARIO =====

CREATE TABLE IF NOT EXISTS Usuario (
    usuario_id UUID PRIMARY KEY,
    email VARCHAR(50) NOT NULL UNIQUE,
    contrasenia VARCHAR(70) NOT NULL,
    nvd_api_key VARCHAR(100)
);

-- ===== AUDITORIA =====

CREATE TABLE IF NOT EXISTS Auditoria (
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

-- ===== ESCANEO =====

CREATE TABLE IF NOT EXISTS Escaneo (
    escaneo_id UUID PRIMARY KEY,
    auditoria_id UUID NOT NULL,

    objetivos VARCHAR(1000) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    progreso INTEGER NOT NULL DEFAULT 0,

    modulo_job_id VARCHAR(100),
    nmap_version TEXT,
    mensaje_error TEXT,

    iniciado_a TIMESTAMP,
    completado_a TIMESTAMP,
    creado_a TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    resultado CLOB,

    CONSTRAINT fk_escaneo_auditoria
        FOREIGN KEY (auditoria_id)
            REFERENCES Auditoria(auditoria_id),

    CONSTRAINT chk_escaneo_progreso
        CHECK (progreso BETWEEN 0 AND 100)
);

-- ===== ACTIVO =====

CREATE TABLE IF NOT EXISTS Activo (
    activo_id UUID PRIMARY KEY,
    escaneo_id UUID NOT NULL,

    host VARCHAR(45) NOT NULL,
    hostname TEXT,
    so TEXT,
    so_probab INTEGER,

    CONSTRAINT chk_activo_so_probab
        CHECK (so_probab BETWEEN 0 AND 100),
    mac VARCHAR(17),
    descripcion TEXT,

    CONSTRAINT fk_activo_escaneo
        FOREIGN KEY (escaneo_id)
            REFERENCES Escaneo(escaneo_id)
);

-- ===== CPE =====

CREATE TABLE IF NOT EXISTS Cpe (
    cpe_id UUID PRIMARY KEY,

    uri VARCHAR(170) NOT NULL UNIQUE,
    uri_legible TEXT NOT NULL,
    servicio_nombre TEXT,
    vendor TEXT,
    producto TEXT,
    version TEXT,
    ultimo_check TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===== CVE =====

CREATE TABLE IF NOT EXISTS Cve (
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

CREATE TABLE IF NOT EXISTS Puerto (
    puerto_id UUID PRIMARY KEY,
    activo_id UUID NOT NULL,

    servicio_fallback TEXT,
    numero INTEGER NOT NULL,
    protocolo VARCHAR(10) NOT NULL,
    estado VARCHAR(10) NOT NULL,

    CONSTRAINT chk_puerto_numero
        CHECK (numero BETWEEN 1 AND 65535),

    CONSTRAINT fk_puerto_activo
        FOREIGN KEY (activo_id)
            REFERENCES Activo(activo_id),

    CONSTRAINT uq_puerto_activo_numero_protocolo_estado
        UNIQUE (activo_id, numero, protocolo, estado)
);

-- ===== PUERTO_CPE (M:N) =====

CREATE TABLE IF NOT EXISTS Puerto_Cpe (
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

CREATE TABLE IF NOT EXISTS Cpe_Cve (
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

CREATE TABLE IF NOT EXISTS Referencia (
    referencia_id UUID PRIMARY KEY,
    cve_id UUID NOT NULL,

    url TEXT NOT NULL,
    source TEXT,
    tags VARCHAR(1000),

    CONSTRAINT fk_referencia_cve
        FOREIGN KEY (cve_id)
            REFERENCES Cve(cve_id)
);

-- ===== CWE =====

CREATE TABLE IF NOT EXISTS Cwe (
    cwe_id UUID PRIMARY KEY,

    cwe_code VARCHAR(20) NOT NULL UNIQUE,
    descripcion TEXT
);

-- ===== CVE_CWE (M:N) =====

CREATE TABLE IF NOT EXISTS Cve_Cwe (
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
