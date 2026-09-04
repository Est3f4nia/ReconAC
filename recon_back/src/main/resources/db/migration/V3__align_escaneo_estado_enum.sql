-- Align existing installations with the enum names used by EscaneoEstado.
-- V2 already creates these values on a fresh database. This migration makes
-- upgrades safe when an earlier local schema created the enum with other labels.

ALTER TYPE estado_escaneo_enum ADD VALUE IF NOT EXISTS 'PENDIENTE';
ALTER TYPE estado_escaneo_enum ADD VALUE IF NOT EXISTS 'EN_PROCESO';
ALTER TYPE estado_escaneo_enum ADD VALUE IF NOT EXISTS 'COMPLETADO';
ALTER TYPE estado_escaneo_enum ADD VALUE IF NOT EXISTS 'FALLO';
