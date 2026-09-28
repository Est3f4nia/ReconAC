import {
  apiFetch,
  assertApiResponseOk,
  parseApiResponse,
} from "./client";

import type {
  EscaneoResponse,
  EscaneoResultResponse,
  MoverEscaneoRequest,
  ScanStartRequest,
  ScanStatusResponse,
  PageEscaneoListadoResponse
} from "./types";

export async function fetchEscaneo(
  auditoriaId: string,
  escaneoId: string,
): Promise<EscaneoResultResponse> {
  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}/escaneos/${encodeURIComponent(escaneoId)}/resultado`,
  );

  return parseApiResponse<EscaneoResultResponse>(res);
}

export async function fetchEscaneos(
  page = 0,
  size = 20,
): Promise<PageEscaneoListadoResponse> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "creadoA,desc",
  });

  const res = await apiFetch(
    `/api/escaneos?${params.toString()}`,
  );

  return parseApiResponse<PageEscaneoListadoResponse>(res);
}

export async function startEscaneo(
  auditoriaId: string,
  request: ScanStartRequest,
): Promise<EscaneoResponse> {
  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}/escaneos`,
    {
      method: "POST",
      body: JSON.stringify(request),
    },
  );

  return parseApiResponse<EscaneoResponse>(res);
}

export async function fetchEscaneoStatus(
  auditoriaId: string,
  escaneoId: string,
): Promise<ScanStatusResponse> {
  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}/escaneos/${encodeURIComponent(escaneoId)}/status`,
  );

  return parseApiResponse<ScanStatusResponse>(res);
}

export async function moveEscaneo(
  escaneoId: string,
  auditoriaId: string,
): Promise<void> {
  const request: MoverEscaneoRequest = {
    auditoriaId,
  };

  const res = await apiFetch(
    `/api/escaneos/${encodeURIComponent(escaneoId)}`,
    {
      method: "PATCH",
      body: JSON.stringify(request),
    },
  );

  await parseApiResponse<void>(res);
}

export async function deleteEscaneo(
  auditoriaId: string,
  escaneoId: string,
): Promise<void> {
  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}/escaneos/${encodeURIComponent(escaneoId)}`,
    {
      method: "DELETE",
    },
  );

  await assertApiResponseOk(
    res,
    "No se pudo eliminar el escaneo.",
  );
}

/* ---------- Reportes ---------- */

export type FormatoReporte = "MD" | "CSV";

export async function descargarReporte(
  auditoriaId: string,
  escaneoId: string,
  formato: FormatoReporte,
): Promise<Blob> {
  const params = new URLSearchParams({
    formato,
  });

  const res = await apiFetch(
    `/api/${encodeURIComponent(auditoriaId)}/escaneos/${encodeURIComponent(escaneoId)}/reporte?${params.toString()}`,
  );

  await assertApiResponseOk(
    res,
    "No se pudo generar el reporte.",
  );

  return res.blob();
}