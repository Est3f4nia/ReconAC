import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { AuthError } from "@/data/auth";
import "@/pages/global.css";
import "@/components/layout/auth/AuthStyle.css";
import "@/pages/public/styles/AuthPage.css";

export default function AuthPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { login, register } = useAuth();

  const isRegister = location.pathname === "/register";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [legal, setLegal] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (isRegister && password !== confirmPassword) {
      setError("Las contraseñas no coinciden");
      return;
    }

    setSubmitting(true);
    try {
      if (isRegister) {
        await register(email, password);
        navigate("/");
      } else {
        await login(email, password);
        navigate("/dashboard");
      }
    } catch (err) {
      if (err instanceof AuthError) {
        setError(err.errors?.[0] ?? err.message);
      } else {
        setError("Error de conexión");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>{isRegister ? "Registrarse" : "Iniciar Sesión"}</h1>

        {error && <p className="auth-error">{error}</p>}

        <form onSubmit={handleSubmit}>
          <div className="auth-field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="auth-field">
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete={isRegister ? "new-password" : "current-password"}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          {isRegister && (
            <>
              <div className="auth-field">
                <label htmlFor="confirmPassword">Repetir contraseña</label>
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type="password"
                  autoComplete="new-password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
              </div>

              <div className="legal-check">
                <input
                  id="legal"
                  type="checkbox"
                  name="legal"
                  required
                  checked={legal}
                  onChange={(e) => setLegal(e.target.checked)}
                />
                <label htmlFor="legal">
                  Acepto el <a href="#">Acuerdo Legal</a>
                </label>
              </div>
            </>
          )}

          {!isRegister && (
            <Link to="/forgot-password" className="forgot-password">
              ¿Olvidaste tu contraseña?
            </Link>
          )}

          <button
            type="submit"
            className="auth-button"
            disabled={submitting}
          >
            {submitting
              ? "Cargando..."
              : isRegister
                ? "Crear cuenta"
                : "Iniciar Sesión"}
          </button>
        </form>

        <div className="auth-switch">
          {isRegister ? (
            <>
              ¿Ya tenés una cuenta?{" "}
              <Link to="/">Iniciar sesión</Link>
            </>
          ) : (
            <>
              ¿No tenés una cuenta?{" "}
              <Link to="/register">Registrarse</Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
