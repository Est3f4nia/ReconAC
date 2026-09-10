

ALTER TABLE puerto
ALTER COLUMN estado TYPE text;

DROP TYPE estado_puerto_enum;

CREATE TYPE estado_puerto_enum AS ENUM (
    'OPEN',
    'CLOSED',
    'FILTERED',
    'UNFILTERED',
    'OPEN_OR_FILTERED',
    'CLOSED_OR_FILTERED'
);

UPDATE puerto
SET estado = UPPER(estado);

ALTER TABLE puerto
ALTER COLUMN estado TYPE estado_puerto_enum
    USING estado::estado_puerto_enum;