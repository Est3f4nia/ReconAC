import { apiFetch } from "./client";
import type { DashboardAuditoria, AuditoriaResponse } from "./types";

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

export async function eliminarAuditoria(
  auditoriaId: string,
): Promise<void> {
  const res = await apiFetch(
    `/api/auditorias/${auditoriaId}`,
    {
      method: "DELETE",
    },
  );

  if (!res.ok) {
    let message =
      "No se pudo eliminar la auditoría.";

    try {
      const body = await res.json();

      message =
        body.detail ??
        body.message ??
        message;
    } catch {
      // DELETE puede no devolver JSON.
    }

    throw new Error(message);
  }
}

export async function updateAuditoria(
  auditoriaId: string,
  nombre: string,
  objetivo: string,
): Promise<AuditoriaResponse> {
  const payload = {
    // El backend aplica "Nueva auditoría" como nombre
    nombre: nombre.trim() || undefined,
    objetivo: objetivo.trim(),
  };

  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}`,
    {
      method: "PATCH",
      body: JSON.stringify(payload),
    },
  );

  return parseResponse<AuditoriaResponse>(res);
}

/**
 * Response ==============
 */

async function parseResponse<T>(res: Response): Promise<T> {
  const body = await res.json();

  if (!res.ok) {
    const detail =
      body.detail ?? body.message ?? "Error desconocido";

    const errors: string[] | undefined = body.errors;

    throw new Error(errors?.[0] ?? detail);
  }

  return (body.data ?? body) as T;
}