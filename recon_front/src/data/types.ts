/* ============================================================
 *  Tipos TS que espejan los DTOs del backend
 * ============================================================ */

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

/* ---------- JWT decodificado ---------- */

export interface JwtPayload {
  sub: string;
  roles: string[];
  iat: number;
  exp: number;
}
