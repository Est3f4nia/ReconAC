import { apiFetch } from "./client";
import type { BaseResponse } from "./types";

export type AuditStatus = "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED";

export interface EscaneoResumen {
  escaneoId: string | null;
  auditoriaId: string;
  auditoriaNombre: string;
  activos: number;
  puertos: number;
  ultimoEscaneo: string | null;
  status: AuditStatus;
  cve: number;
  cveCriticos: number;
}

export async function fetchResumen(): Promise<EscaneoResumen[]> {
  const res = await apiFetch("/api/auditorias/resumen");
  if (!res.ok) throw new Error("Error al obtener resumen");
  const body: BaseResponse<EscaneoResumen[]> = await res.json();
  return body.data;
}