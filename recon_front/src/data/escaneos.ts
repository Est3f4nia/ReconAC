import { apiFetch } from "./client";
import type {
  AuditoriaResponse,
  EscaneoResumen,
  EscaneoResultResponse,
  EscaneoResponse,
  ScanStartRequest,
  ScanStatusResponse,
  PageResponse,
  EscaneoListado
} from "./types";

export async function fetchResumen(): Promise<EscaneoResumen[]> {
  const res = await apiFetch("/api/auditorias/resumen");

  return parseResponse<EscaneoResumen[]>(res);
}

export async function createAuditoria(
  nombre: string,
  objetivo: string,
): Promise<AuditoriaResponse> {
  const res = await apiFetch("/api/auditorias", {
    method: "POST",
    body: JSON.stringify({ nombre, objetivo }),
  });

  return parseResponse<AuditoriaResponse>(res);
}

export async function fetchAuditoria(
  auditoriaId: string,
): Promise<AuditoriaResponse> {
  const res = await apiFetch(`/api/auditorias/${auditoriaId}`);

  return parseResponse<AuditoriaResponse>(res);
}


export async function fetchAuditorias(): Promise<AuditoriaResponse[]> {
  const res = await apiFetch("/api/auditorias?size=100");

  const body = await res.json();

  if (!res.ok) {
    const detail = body.detail ?? body.message ?? "Error desconocido";
    throw new Error(detail);
  }

  return body.content ?? body.data ?? [];
}


export async function fetchEscaneo(
  auditoriaId: string,
  escaneoId: string,
): Promise<EscaneoResultResponse> {
  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}/escaneos/${encodeURIComponent(escaneoId)}/resultado`,
  );

  return parseResponse<EscaneoResultResponse>(res);
}


export async function fetchEscaneos(
  page = 0,
  size = 20,
): Promise<PageResponse<EscaneoListado>> {
  const res = await apiFetch(
    `/api/escaneos?page=${page}&size=${size}&sort=creadoA,desc`,
  );

  return parseResponse<PageResponse<EscaneoListado>>(res);
}


export async function moveEscaneo(
  escaneoId: string,
  auditoriaId: string,
): Promise<void> {
  const res = await apiFetch(
    `/api/escaneos/${encodeURIComponent(escaneoId)}`,
    {
      method: "PATCH",
      body: JSON.stringify({ auditoriaId }),
    },
  );

  await parseResponse<void>(res);
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

  await parseResponse<void>(res);
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

  return parseResponse<EscaneoResponse>(res);
}

export async function fetchEscaneoStatus(
  auditoriaId: string,
  escaneoId: string,
): Promise<ScanStatusResponse> {
  const res = await apiFetch(
    `/api/auditorias/${encodeURIComponent(auditoriaId)}/escaneos/${encodeURIComponent(escaneoId)}/status`,
  );

  return parseResponse<ScanStatusResponse>(res);
}

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


