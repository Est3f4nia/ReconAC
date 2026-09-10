-- Detectar un CPE no equivale a haber completado una consulta NVD.
ALTER TABLE cpe ALTER COLUMN ultimo_check DROP NOT NULL;
ALTER TABLE cpe ALTER COLUMN ultimo_check DROP DEFAULT;

-- La caché anterior no distinguía los resultados completos de los incompletos.
-- Revalidar una vez, conservando CPE, CVE y sus relaciones.
UPDATE cpe SET ultimo_check = NULL;
