/* ============================================================
 *  Tipos TS que espejan los DTOs del backend
 * ============================================================ */

export type Estado =
  | "PENDIENTE"
  | "EN_PROCESO"
  | "COMPLETADO"
  | "FALLO";

/** Envoltorio estándar del back (BaseResponse) */
export interface BaseResponse<T> {
  data: T;
  message: string;
}

/* ---------- Auth ---------- */

export interface AuthResponse {
  id: string;
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  email: string;
  csrfToken: string;
}

export interface RefreshResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  csrfToken: string;
}

/* ---------- Auditoría ---------- */

export interface AuditoriaResponse {
  id: string;
  nombre: string;
  objetivo: string;
  fechaGeneracion: string;
  fechaFinal: string | null;
}

/* ---------- JWT decodificado ---------- */

export interface JwtPayload {
  sub: string;
  roles: string[];
  iat: number;
  exp: number;
}

/* ---------- Escaneo ---------- */

export interface EscaneoResumen {
  escaneoId: string | null;
  auditoriaId: string;
  auditoriaNombre: string;
  activos: number;
  puertos: number;
  completadoA: string | null;
  status: Estado | null;
  cve: number;
  cveCriticos: number;
}

export interface HostResult {
  ip: string;
  mac: string | null;
  hostname: string | null;
  os: string | null;
}

export interface EscaneoResult {
  hosts: HostResult[];
  apiResults: Record<string, unknown>;
  nmapVersion: string | null;
  startTime: string | null;
  endTime: string | null;
}

export interface EscaneoResponse {
  activos: string[];
  estado: Estado;
  progreso: number;
  nmapVersion: string | null;
  mensajeError: string | null;
  iniciadoA: string | null;
  completadoA: string | null;
  creadoA: string;
}

export interface ScanStartRequest {
  objetivos: string[];
  nvdApiKey?: string;
}

export interface ScanStatusResponse {
  scanId: string | null;
  status: Estado;
  progress: number;
  error: string | null;
}

export interface EscaneoResultResponse {
  escaneo: EscaneoResponse;
  resultado: EscaneoResult | null;
}

export interface EscaneoListado {
  escaneoId: string;
  auditoriaId: string;
  auditoriaNombre: string;
  objetivos: string[];
  estado: Estado;
  progreso: number;
  nmapVersion: string | null;
  iniciadoA: string | null;
  completadoA: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
