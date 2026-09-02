import { apiFetch } from "./client";
import type { BaseResponse, AuditoriaResponse } from "./types";

export type AuditStatus = "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED";

export interface EscaneoResumen {
  escaneoId: string | null;
  auditoriaId: string;
  auditoriaNombre: string;
  activos: number;
  puertos: number;
  completadoA: string | null;
  status: AuditStatus | null;
  cve: number;
  cveCriticos: number;
}

export async function fetchResumen(): Promise<EscaneoResumen[]> {
  const res = await apiFetch("/api/auditorias/resumen");
  if (!res.ok) {
    const body = await res.json();
    const detail = body.detail ?? body.message ?? "Error al obtener resumen";
    throw new Error(detail);
  }
  const body: BaseResponse<EscaneoResumen[]> = await res.json();
  return body.data ?? [];
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

async function parseResponse<T>(res: Response): Promise<T> {
  const body = await res.json();
  if (!res.ok) {
    const detail = body.detail ?? body.message ?? "Error desconocido";
    const errors: string[] | undefined = body.errors;
    throw new Error(errors?.[0] ?? detail);
  }
  return (body.data ?? body) as T;
}