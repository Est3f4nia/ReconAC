import { Link, useLocation } from "react-router-dom";
import "@/pages/global.css";
import "@/components/layout/auth/AuthStyle.css";
import "@/pages/public/styles/AuthPage.css";

export default function AuthPage() {
  const location = useLocation();

  const isRegister = location.pathname === "/register";

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>{isRegister ? "Registrarse" : "Iniciar Sesión"}</h1>

        <form>
          <div className="auth-field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              required
            />
          </div>

          <div className="auth-field">
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete={
                isRegister ? "new-password" : "current-password"
              }
              required
            />
          </div>

          {isRegister && (
            <>
              <div className="auth-field">
                <label htmlFor="confirmPassword">
                  Repetir contraseña
                </label>

                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type="password"
                  autoComplete="new-password"
                  required
                />
              </div>

              <div className="legal-check">
                    <input
                        id="legal"
                        type="checkbox"
                        name="legal"
                        required
                    />

                    <label htmlFor="legal">
                        Acepto el <a href="#">Acuerdo Legal</a>
                    </label>
                </div>
              
            </>
          )}

          {!isRegister && (
            <Link
              to="/forgot-password"
              className="forgot-password"
            >
              ¿Olvidaste tu contraseña?
            </Link>
          )}

          <button type="submit" className="auth-button">
            {isRegister ? "Crear cuenta" : "Iniciar Sesión"}
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