/* ============================================================
 *  Funciones de autenticación — llaman a /api/auth/*
 * ============================================================ */

import { apiFetch } from "./client";
import type { AuthResponse, BaseResponse, RefreshResponse } from "./types";

export class AuthError extends Error {
  status: number;
  errors?: string[];

  constructor(status: number, message: string, errors?: string[]) {
    super(message);
    this.name = "AuthError";
    this.status = status;
    this.errors = errors;
  }
}

async function parseResponse<T>(res: Response): Promise<T> {
  const body = await res.json();

  if (!res.ok) {
    const detail = body.detail ?? body.message ?? "Error desconocido";
    const errors: string[] | undefined = body.errors;
    throw new AuthError(res.status, detail, errors);
  }

  return (body.data ?? body) as T;
}

export async function login(
  email: string,
  contrasenia: string,
): Promise<AuthResponse> {
  const res = await apiFetch("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, contrasenia }),
  });
  return parseResponse<AuthResponse>(res);
}

export async function register(
  email: string,
  contrasenia: string,
  apiKey: string
): Promise<void> {
  const res = await apiFetch("/api/auth/register", {
    method: "POST",
    body: JSON.stringify({ email, contrasenia, apiKey }),
  });
  await parseResponse<BaseResponse<null>>(res);
}

export async function refresh(
  refreshToken: string,
): Promise<RefreshResponse> {
  const res = await apiFetch("/api/auth/refresh", {
    method: "POST",
    body: JSON.stringify({ refreshToken }),
  });
  return parseResponse<RefreshResponse>(res);
}
