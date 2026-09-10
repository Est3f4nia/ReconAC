import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import "./Navbar.css";

export interface NavbarItem {
  label: string;
  path: string;
}

interface NavbarProps {
  items?: NavbarItem[];
}

export function Navbar({ items = [] }: NavbarProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/");
  }

  return (
    <header className="navbar">
      <Link to="/dashboard" className="navbar-brand">
        ReconAC
      </Link>

      <nav className="navbar-navigation">
        {items.map((item) => (
          <Link key={item.path} to={item.path} className="navbar-link">
            {item.label}
          </Link>
        ))}
      </nav>

      <div className="navbar-user">
        <span className="navbar-avatar">
          {user?.email.charAt(0).toUpperCase() ?? "U"}
        </span>
        <button className="button navbar-logout" onClick={handleLogout}>
          Salir
        </button>
      </div>
    </header>
  );
}
