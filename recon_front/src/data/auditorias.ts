import { apiFetch } from "./client";
import type { DashboardAuditoria } from "./types";

export async function fetchDashboardAuditoria(
  auditoriaId: string,
): Promise<DashboardAuditoria> {
  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}/dashboard`,
  );

  const body = await res.json();

  if (!res.ok) {
    const detail =
      body.detail ??
      body.message ??
      body.errors ??
      "No se pudo obtener el dashboard de la auditoría.";

    throw new Error(
      typeof detail === "string"
        ? detail
        : "Error en la petición.",
    );
  }

  return body.data ?? body;
}