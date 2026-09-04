-- V3__change_activo_mac_to_text.sql

ALTER TABLE activo
ALTER COLUMN mac TYPE TEXT
    USING mac::TEXT;