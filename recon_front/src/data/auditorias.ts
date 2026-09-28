import {
  apiFetch,
  assertApiResponseOk,
  parseApiResponse,
} from "./client";

import type {
  AuditoriaEstadisticasResponse,
  AuditoriaRequest,
  AuditoriaResponse,
  AuditoriaResumen,
  DashboardAuditoria,
  PageAuditoriaResponse,
} from "./types";

export type { AuditoriaResumen } from "./types";

export async function createAuditoria(
  nombre: string,
  objetivo: string,
): Promise<AuditoriaResponse> {
  const request: AuditoriaRequest = {
    nombre,
    objetivo,
  };

  const res = await apiFetch("/api/auditorias", {
    method: "POST",
    body: JSON.stringify(request),
  });

  return parseApiResponse<AuditoriaResponse>(res);
}

export async function fetchAuditoria(
  auditoriaId: string,
): Promise<AuditoriaResponse> {
  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}`,
  );

  return parseApiResponse<AuditoriaResponse>(res);
}

export async function fetchAuditorias(): Promise<AuditoriaResponse[]> {
  const res = await apiFetch(
    "/api/auditorias?size=100",
  );

  const page =
    await parseApiResponse<PageAuditoriaResponse>(res);

  return page.content ?? [];
}

export async function updateAuditoria(
  auditoriaId: string,
  nombre: string,
  objetivo: string,
): Promise<AuditoriaResponse> {
  const request: AuditoriaRequest = {
    nombre: nombre.trim() || undefined,
    objetivo: objetivo.trim(),
  };

  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}`,
    {
      method: "PATCH",
      body: JSON.stringify(request),
    },
  );

  return parseApiResponse<AuditoriaResponse>(res);
}

export async function eliminarAuditoria(
  auditoriaId: string,
): Promise<void> {
  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}`,
    {
      method: "DELETE",
    },
  );

  await assertApiResponseOk(
    res,
    "No se pudo eliminar la auditoría.",
  );
}

export async function fetchResumenAuditorias(): Promise<AuditoriaResumen[]> {
  const res = await apiFetch(
    "/api/auditorias/resumen",
  );

  return parseApiResponse<AuditoriaResumen[]>(res);
}

export async function fetchAuditoriaEstadisticas(
  auditoriaId: string,
): Promise<AuditoriaEstadisticasResponse> {
  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}/estadisticas`,
  );

  return parseApiResponse<AuditoriaEstadisticasResponse>(
    res,
  );
}

export async function fetchDashboardAuditoria(
  auditoriaId: string,
): Promise<DashboardAuditoria> {
  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}/dashboard`,
  );

  return parseApiResponse<DashboardAuditoria>(
    res,
    "No se pudo obtener el dashboard de la auditoría.",
  );
}