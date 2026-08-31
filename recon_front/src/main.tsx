import { StrictMode } from "react";  // buenas prácticas dev
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "@/App";
// import "@/styles/global.css";  // ver aplicación

// busca id="root" con non-null assertion (asume que getElementById no devuelve null)
createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>
);