import {
  apiFetch,
  parseApiResponse,
  requestTokenRefresh,
} from "./client";

import type {
  AuthResponse,
  LoginRequest,
  RefreshResponse,
  RegisterRequest,
} from "./types";

export class AuthError extends Error {
  status: number;
  errors?: string[];

  constructor(
    status: number,
    message: string,
    errors?: string[],
  ) {
    super(message);
    this.name = "AuthError";
    this.status = status;
    this.errors = errors;
  }
}

export async function login(
  email: string,
  contrasenia: string,
): Promise<AuthResponse> {
  const request: LoginRequest = {
    email,
    contrasenia,
  };

  const res = await apiFetch("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(request),
  });

  return parseApiResponse<AuthResponse>(
    res,
    "No se pudo iniciar sesión.",
    (status, message, errors) =>
      new AuthError(status, message, errors),
  );
}

export async function register(
  email: string,
  contrasenia: string,
  apiKey: string,
): Promise<void> {
  const request: RegisterRequest = {
    email,
    contrasenia,
    apiKey,
  };

  const res = await apiFetch("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(request),
  });

  await parseApiResponse<void>(
    res,
    "No se pudo registrar el usuario.",
    (status, message, errors) =>
      new AuthError(status, message, errors),
  );
}

export function refresh(
  refreshToken: string,
): Promise<RefreshResponse> {
  return requestTokenRefresh(refreshToken);
}