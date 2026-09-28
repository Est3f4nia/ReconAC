/* ============================================================
 * Fetch wrapper — CSRF (Double Submit Cookie) + auto-refresh
 * ============================================================ */

import type {
  JwtPayload,
  RefreshRequest,
  RefreshResponse,
} from "./types";

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";
const MUTATING = new Set(["POST", "PUT", "PATCH", "DELETE"]);
const BEARER_PREFIX = "Bearer ";

const STORAGE_KEY_ACCESS = "reconac_access_token";
const STORAGE_KEY_REFRESH = "reconac_refresh_token";

/* ---------- Estado de autenticación ---------- */

let accessToken: string | null =
  sessionStorage.getItem(STORAGE_KEY_ACCESS);

let refreshToken: string | null =
  sessionStorage.getItem(STORAGE_KEY_REFRESH);

let refreshInFlight: Promise<boolean> | null = null;

export function setTokens(
  access: string,
  refresh: string,
): void {
  accessToken = access;
  refreshToken = refresh;

  sessionStorage.setItem(STORAGE_KEY_ACCESS, access);
  sessionStorage.setItem(STORAGE_KEY_REFRESH, refresh);
}

export function clearTokens(): void {
  accessToken = null;
  refreshToken = null;

  sessionStorage.removeItem(STORAGE_KEY_ACCESS);
  sessionStorage.removeItem(STORAGE_KEY_REFRESH);
}

export function getAccessToken(): string | null {
  return accessToken;
}

/* ---------- Utilidades ---------- */

function getCookie(name: string): string | null {
  const match = document.cookie.match(
    new RegExp(`(^| )${name}=([^;]+)`),
  );

  return match
    ? decodeURIComponent(match[2])
    : null;
}

export function decodeJwt(
  token: string,
): JwtPayload | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;

    const base64 = payload
      .replace(/-/g, "+")
      .replace(/_/g, "/");

    const padded = base64.padEnd(
      Math.ceil(base64.length / 4) * 4,
      "=",
    );

    return JSON.parse(
      atob(padded),
    ) as JwtPayload;
  } catch {
    return null;
  }
}

/* ---------- Response ---------- */

type ApiErrorFactory = (
  status: number,
  message: string,
  errors?: string[],
) => Error;

async function readJson(
  res: Response,
): Promise<any> {
  return res.json().catch(() => null);
}

function getErrors(body: any): string[] | undefined {
  return Array.isArray(body?.errors)
    ? body.errors
    : undefined;
}

function getErrorMessage(
  body: any,
  fallback: string,
): string {
  const errors = getErrors(body);

  const detail =
    body?.detail ??
    body?.message ??
    errors?.[0] ??
    fallback;

  return typeof detail === "string"
    ? detail
    : fallback;
}

export async function parseApiResponse<T>(
  res: Response,
  fallback = "Error desconocido",
  errorFactory?: ApiErrorFactory,
): Promise<T> {
  const body = await readJson(res);

  if (!res.ok) {
    const errors = getErrors(body);
    const message = getErrorMessage(body, fallback);

    throw errorFactory
      ? errorFactory(res.status, message, errors)
      : new Error(message);
  }

  if (
    body !== null &&
    typeof body === "object" &&
    "data" in body
  ) {
    return body.data as T;
  }

  return body as T;
}

export async function assertApiResponseOk(
  res: Response,
  fallback = "Error desconocido",
): Promise<void> {
  if (res.ok) return;

  const body = await readJson(res);

  throw new Error(
    getErrorMessage(body, fallback),
  );
}

/* ---------- Fetch ---------- */

function buildHeaders(
  method: string,
  options: RequestInit,
): Headers {
  const headers = new Headers(options.headers);

  if (MUTATING.has(method)) {
    const csrf = getCookie(CSRF_COOKIE);

    if (csrf) {
      headers.set(CSRF_HEADER, csrf);
    }
  }

  if (
    !headers.has("Content-Type") &&
    options.body
  ) {
    headers.set(
      "Content-Type",
      "application/json",
    );
  }

  if (accessToken) {
    headers.set(
      "Authorization",
      BEARER_PREFIX + accessToken,
    );
  }

  return headers;
}

function performFetch(
  url: string,
  method: string,
  options: RequestInit,
): Promise<Response> {
  return fetch(url, {
    ...options,
    method,
    headers: buildHeaders(method, options),
    credentials: "include",
  });
}

/* ---------- Refresh ---------- */

export async function requestTokenRefresh(
  token: string,
): Promise<RefreshResponse> {
  const request: RefreshRequest = {
    refreshToken: token,
  };

  const headers = new Headers({
    "Content-Type": "application/json",
  });

  const csrf = getCookie(CSRF_COOKIE);

  if (csrf) {
    headers.set(CSRF_HEADER, csrf);
  }

  const res = await fetch("/api/auth/refresh", {
    method: "POST",
    credentials: "include",
    headers,
    body: JSON.stringify(request),
  });

  const data = await parseApiResponse<RefreshResponse>(
    res,
    "No se pudo renovar la sesión.",
  );

  if (!data?.accessToken) {
    throw new Error(
      "La respuesta de refresh no contiene accessToken.",
    );
  }

  return data;
}

async function doRefresh(): Promise<boolean> {
  if (!refreshToken) return false;

  if (!refreshInFlight) {
    refreshInFlight = requestTokenRefresh(refreshToken)
      .then((data) => {
        if (!data.accessToken) return false;

        accessToken = data.accessToken;

        sessionStorage.setItem(
          STORAGE_KEY_ACCESS,
          data.accessToken,
        );

        return true;
      })
      .catch(() => false)
      .finally(() => {
        refreshInFlight = null;
      });
  }

  return refreshInFlight;
}

/* ---------- API Fetch ---------- */

export async function apiFetch(
  url: string,
  options: RequestInit = {},
): Promise<Response> {
  const method = (
    options.method ?? "GET"
  ).toUpperCase();

  let res = await performFetch(
    url,
    method,
    options,
  );

  if (
    res.status === 401 &&
    refreshToken &&
    url !== "/api/auth/refresh"
  ) {
    const refreshed = await doRefresh();

    if (refreshed) {
      res = await performFetch(
        url,
        method,
        options,
      );
    }
  }

  return res;
}