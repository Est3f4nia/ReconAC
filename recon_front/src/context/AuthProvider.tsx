import { createContext, useCallback, useEffect, useMemo, useState } from "react";
import * as authApi from "@/data/auth";
import { setTokens, clearTokens, decodeJwt, getAccessToken } from "@/data/client";
import type { JwtPayload } from "@/data/types";

export interface AuthUser {
  email: string;
  roles: string[];
}

export interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, contrasenia: string) => Promise<void>;
  register: (email: string, contrasenia: string) => Promise<void>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

function userFromToken(token: string): AuthUser | null {
  const payload: JwtPayload | null = decodeJwt(token);
  if (!payload?.sub) return null;
  return { email: payload.sub, roles: payload.roles ?? [] };
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = getAccessToken();
    if (token) {
      const u = userFromToken(token);
      if (u) setUser(u);
    }
    setLoading(false);
  }, []);

  const login = useCallback(async (email: string, contrasenia: string) => {
    const res = await authApi.login(email, contrasenia);
    setTokens(res.accessToken, res.refreshToken);
    setUser({ email: res.email, roles: decodeJwt(res.accessToken)?.roles ?? [] });
  }, []);

  const register = useCallback(async (email: string, contrasenia: string) => {
    await authApi.register(email, contrasenia);
  }, []);

  const logout = useCallback(() => {
    clearTokens();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, isAuthenticated: user !== null, loading, login, register, logout }),
    [user, loading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
