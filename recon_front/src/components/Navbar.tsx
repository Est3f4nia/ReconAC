import { Link } from "react-router-dom";
// import "./Navbar.css";  // ver

export interface NavbarItem {
  label: string;
  path: string;
}

interface NavbarProps {
  items?: NavbarItem[];
  username?: string;
}

export function Navbar({
  items = [],
  username,
}: NavbarProps) {
  return (
    <header className="navbar">
      <Link to="/dashboard" className="navbar-brand">
        ReconAC
      </Link>

      <nav className="navbar-navigation">
        {items.map((item) => (
          <Link
            key={item.path}
            to={item.path}
            className="navbar-link"
          >
            {item.label}
          </Link>
        ))}
      </nav>

      <div className="navbar-user">
        <span className="navbar-avatar">
          {username?.charAt(0).toUpperCase() ?? "U"}
        </span>
      </div>
    </header>
  );
}