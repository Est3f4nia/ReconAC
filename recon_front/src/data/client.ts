/* ============================================================
 *  Fetch wrapper — CSRF (Double Submit Cookie) + auto-refresh
 * ============================================================ */

import type { AuthResponse, JwtPayload } from "./types";

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";
const MUTATING = new Set(["POST", "PUT", "PATCH", "DELETE"]);

/* ---------- estado en memoria (singleton) ---------- */

let accessToken: string | null = null;
let refreshToken: string | null = null;

export function setTokens(access: string, refresh: string) {
  accessToken = access;
  refreshToken = refresh;
}

export function clearTokens() {
  accessToken = null;
  refreshToken = null;
}

export function getAccessToken(): string | null {
  return accessToken;
}

/* ---------- utilidades ---------- */

function getCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp("(^| )" + name + "=([^;]+)"));
  return match ? decodeURIComponent(match[2]) : null;
}

export function decodeJwt(token: string): JwtPayload | null {
  try {
    const base64 = token.split(".")[1];
    const json = atob(base64.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

function isExpired(token: string, skewMs = 5000): boolean {
  const payload = decodeJwt(token);
  if (!payload?.exp) return true;
  return Date.now() >= payload.exp * 1000 - skewMs;
}

/* ---------- refresh ---------- */

async function doRefresh(): Promise<boolean> {
  if (!refreshToken) return false;

  const res = await fetch("/api/auth/refresh", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) return false;

  const body = await res.json();
  const data = body.data ?? body;
  accessToken = data.accessToken;
  if (data.refreshToken) refreshToken = data.refreshToken;
  return true;
}

/* ---------- apiFetch ---------- */

export async function apiFetch(
  url: string,
  options: RequestInit = {},
): Promise<Response> {
  const method = (options.method ?? "GET").toUpperCase();
  const headers = new Headers(options.headers);

  if (MUTATING.has(method)) {
    const csrf = getCookie(CSRF_COOKIE);
    if (csrf) headers.set(CSRF_HEADER, csrf);
  }

  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }

  let res = await fetch(url, {
    ...options,
    method,
    headers,
    credentials: "include",
  });

  if (res.status === 401 && refreshToken && url !== "/api/auth/refresh") {
    const ok = await doRefresh();
    if (ok) {
      if (MUTATING.has(method)) {
        const csrf = getCookie(CSRF_COOKIE);
        if (csrf) headers.set(CSRF_HEADER, csrf);
      }
      res = await fetch(url, { ...options, method, headers, credentials: "include" });
    }
  }

  return res;
}
