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

export interface AuditoriaEstadisticasResponse {
  totalEscaneos: number;
  escaneosCompletados: number;
  escaneosEnProceso: number;
  escaneosPendientes: number;
  escaneosFallidos: number;
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
  creadoA: string;
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

/* ---------- Activo ---------- */

export interface ActivoResponse {
  id: string;
  escaneoId: string;
  host: string;
  hostname: string | null;
  so: string | null;
  soProbab: number | null;
  mac: string | null;
  descripcion: string | null;
}

export interface ActivoAgrupadoResponse {
    host: string;
    hostname: string | null;
    so: string | null;
    soProbab: number | null;
    mac: string | null;
    descripcion: string | null;
    escaneoIds: string[];
}

export interface ActivoRequest {
  escaneoId: string;
  host: string;
  hostname?: string;
  so?: string;
  soProbab?: number;
  mac?: string;
  descripcion?: string;
}

/* ---------- Métricas ---------- */

export interface DashboardKpis {
  escaneos: number;
  activos: number;
  puertos: number;
  cves: number;

  cvesCriticos: number;
  cvesAltos: number;
  cvesMedios: number;
  cvesBajos: number;

  cvssPromedio: number | null;

  cvesExplotados: number;
  epssPromedio: number | null;
}

export interface RiesgoTemporal {
  escaneoId: string;
  fecha: string;

  nivel: "BAJO" | "MEDIO" | "ALTO" | "CRITICO" | "DESCONOCIDO";
  cvssPromedio: number | null;

  cves: number;
  cvesCriticos: number;
  cvesExplotados: number;
}

export interface CveResumen {
  cveId: string;

  frecuencia: number;

  cvssScore: number | null;
  epssScore: number | null;

  explotacionActiva: boolean;

  cwes: string[];
}

export interface CveDesglose {
  masComunes: CveResumen[];
  explotacionActiva: CveResumen[];
  mayorCriticidad: CveResumen[];
  mayorProbabilidadExplotacion: CveResumen[];
}

export interface HostVulnerabilidad {
  ip: string;
  hostname: string | null;

  vulnerabilidades: number;
  vulnerabilidadesCriticas: number;

  cvssMaximo: number | null;
  epssMaximo: number | null;
}

export interface HostDesglose {
  masVulnerabilidadesCriticas: HostVulnerabilidad[];
  mayorProbabilidadExplotacion: HostVulnerabilidad[];
}

export interface EjecucionHistorial {
  escaneoId: string;
  fecha: string;

  estado: Estado;
  progreso: number;

  objetivos: string[];

  activos: number;
  puertos: number;
  cves: number;
  cvesCriticos: number;

  cvssPromedio: number | null;
}

export interface ComparacionActivo {
  escaneoId: string;
  fecha: string;

  host: string;
  hostname: string | null;

  puertos: number;
  cves: number;
  cvesCriticos: number;

  cvssPromedio: number | null;
}

export interface ComparacionResultado {
  ejecucionA: string;
  ejecucionB: string;

  activosNuevos: number;
  activosEliminados: number;

  puertosNuevos: number;
  puertosEliminados: number;

  cvesNuevos: number;
  cvesResueltos: number;
  cvesPersistentes: number;

  cvssAnterior: number | null;
  cvssActual: number | null;
}

export interface DashboardAuditoria {
  kpis: DashboardKpis;

  historial: EjecucionHistorial[];

  riesgoTemporal: RiesgoTemporal[];

  vulnerabilidades: CveDesglose;

  hosts: HostDesglose;
}