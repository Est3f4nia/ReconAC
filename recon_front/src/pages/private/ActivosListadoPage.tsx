import { useEffect, useState } from "react";
import { fetchActivos } from "@/data/activos";
import type { ActivoAgrupadoResponse } from "@/data/types";
import ActivosListado from "./ActivosListado";

const PAGE_SIZE = 20;

export default function ActivosListadoPage() {
  const [activos, setActivos] = useState<ActivoAgrupadoResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [activoDetalle, setActivoDetalle] =
    useState<ActivoAgrupadoResponse | null>(null);

  const cargarActivos = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await fetchActivos(page, PAGE_SIZE);

      setActivos(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudieron cargar los activos."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void cargarActivos();
  }, [page]);

  const siguientePagina = () => {
    if (page < totalPages - 1) {
      setPage((prev) => prev + 1);
    }
  };

  const paginaAnterior = () => {
    if (page > 0) {
      setPage((prev) => prev - 1);
    }
  };

  if (loading) {
    return (
      <section className="listado-page">
        <div className="listado-header">
          <div>
            <h1>Activos</h1>
            <p>Cargando activos...</p>
          </div>
        </div>
        <div className="listado-loading">Cargando...</div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="listado-page">
        <div className="listado-header">
          <div>
            <h1>Activos</h1>
          </div>
        </div>
        <div className="listado-error">
          <p>{error}</p>
          <button
            type="button"
            className="button button-error"
            onClick={() => void cargarActivos()}
          >
            Reintentar
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="listado-page">
      <div className="listado-header">
        <div>
          <h1>Activos</h1>
          <p>Activos detectados en tus escaneos.</p>
        </div>
        <span className="listado-total">
          {totalElements} {totalElements === 1 ? "activo" : "activos"}
        </span>
      </div>

      <ActivosListado
        activos={activos}
        page={page}
        totalPages={totalPages}
        onAnterior={paginaAnterior}
        onSiguiente={siguientePagina}
        onVerDetalle={setActivoDetalle}
        activoDetalle={activoDetalle}
        onCerrarDetalle={() => setActivoDetalle(null)}
      />
    </section>
  );
}