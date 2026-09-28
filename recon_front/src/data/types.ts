import type { components } from "./api-schema";

type Schemas = components["schemas"];

/* ============================================================
 * Contrato Backend — OpenAPI
 * ============================================================ */

/* ---------- Estados ---------- */

/*
 * Springdoc actualmente genera estos enums inline en los DTO,
 * por eso se derivan de sus propiedades en lugar de duplicarlos.
 */
export type Estado_Escaneo =
  NonNullable<Schemas["EscaneoResponse"]["estado"]>;

export type Estado_Puerto =
  NonNullable<Schemas["PuertoResultadoResponse"]["estado"]>;


/* ---------- Respuestas comunes ---------- */

type BaseResponseSchema = Schemas["BaseResponseVoid"];

export type BaseResponse<T> =
  Omit<BaseResponseSchema, "data"> & {
    data?: T;
  };

/*
 * Spring genera un schema Page distinto por DTO.
 * Conservamos este helper genérico porque simplifica los consumidores
 * del frontend sin volver a duplicar la estructura completa de Page.
 */
type PageSchema = Schemas["PageEscaneoListadoResponse"];

export type PageResponse<T> =
  Omit<PageSchema, "content"> & {
    content?: T[];
  };


/* ---------- Auth ---------- */

export type AuthResponse =
  Schemas["AuthResponseDto"];

export type RefreshResponse =
  Schemas["RefreshResponseDto"];

export type RegisterRequest =
  Schemas["RegisterRequestDto"];

export type LoginRequest =
  Schemas["LoginRequestDto"];

export type RefreshRequest =
  Schemas["RefreshRequestDto"];

export type UpdateNvdApiKeyRequest =
  Schemas["UpdateNvdApiKeyRequest"];

/* ---------- Auditoría ---------- */

export type AuditoriaResponse =
  Schemas["AuditoriaResponse"];

export type AuditoriaRequest =
  Schemas["AuditoriaRequestDto"];

export type AuditoriaEstadisticasResponse =
  Schemas["AuditoriaEstadisticasResponse"];

export type AuditoriaResumen =
  Schemas["AuditoriaResumenResponseDto"];

export type PageAuditoriaResponse =
  Schemas["PageAuditoriaResponse"];


/* ---------- Escaneo ---------- */

export type HostResult =
  Schemas["HostResult"];

export type EscaneoResponse =
  Schemas["EscaneoResponse"];

export type ScanStartRequest =
  Schemas["ScanStartRequest"];

export type ScanStatusResponse =
  Schemas["ScanStatusResponse"];

export type EscaneoResultResponse =
  Schemas["EscaneoResultResponse"];

export type EscaneoListado =
  Schemas["EscaneoListadoResponse"];

export type MoverEscaneoRequest =
  Schemas["MoverEscaneoRequest"];

export type PageEscaneoListadoResponse =
  Schemas["PageEscaneoListadoResponse"];


/* ---------- Activos ---------- */

export type ActivoResponse =
  Schemas["ActivoResponse"];

export type ActivoAgrupadoResponse =
  Schemas["ActivoAgrupadoResponse"];

export type ActivoResultadoResponse =
  Schemas["ActivoResultadoResponse"];

export type PageActivoAgrupadoResponse =
  Schemas["PageActivoAgrupadoResponse"];

export type PageActivoResponse =
  Schemas["PageActivoResponse"];

/* ---------- Puertos ---------- */

export type PuertoResultadoResponse =
  Schemas["PuertoResultadoResponse"];


/* ---------- Vulnerabilidades ---------- */

export type CveResumen =
  Schemas["CveResumenResponse"];

export type CveDetalle =
  Schemas["CveDetalleResponse"];

export type CveDesglose =
  Schemas["CveDesgloseResponse"];

export type HostCve =
  Schemas["HostCveResponse"];

export type HostVulnerabilidad =
  Schemas["HostVulnerabilidadResponse"];

export type HostDesglose =
  Schemas["HostDesgloseResponse"];


/* ---------- Métricas ---------- */

export type DashboardKpis =
  Schemas["DashboardKpisResponse"];

export type RiesgoTemporal =
  Schemas["RiesgoTemporalResponse"];

export type EjecucionHistorial =
  Schemas["EjecucionHistorialResponse"];

export type DashboardAuditoria =
  Schemas["DashboardAuditoriaResponse"];


/* ============================================================
 * Tipos exclusivos del frontend
 * ============================================================ */

/*
 * JwtPayload no forma parte del contrato HTTP documentado por OpenAPI.
 * Representa el contenido que el frontend espera encontrar al decodificar
 * localmente el access token JWT, por eso debe mantenerse como tipo propio.
 *
 * Si cambia la estructura de claims emitida por el backend, revisar este tipo.
 */
export interface JwtPayload {
  sub: string;
  roles: string[];
  iat: number;
  exp: number;
}


/*
 * Tipos utilizados únicamente para construir comparaciones en el frontend.
 * No representan DTOs expuestos actualmente por la API.
 */
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