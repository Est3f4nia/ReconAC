import { apiFetch } from "./client";
import type { CveDetalle } from "./types";

export async function fetchCveDetalle(
  cveId: string,
): Promise<CveDetalle> {
  const res = await apiFetch(
    `/api/vulnerabilities/${encodeURIComponent(cveId)}`,
  );

  const body = await res.json();

  if (!res.ok) {
    const detail =
      body.detail ??
      body.message ??
      body.errors ??
      "No se pudo obtener el detalle de la vulnerabilidad.";

    throw new Error(
      typeof detail === "string"
        ? detail
        : "Error en la petición.",
    );
  }

  return (body.data ?? body) as CveDetalle;
}