import { useEffect, useState } from "react";
import {
  fetchAuditorias,
  fetchEscaneos,
  fetchEscaneo,
  deleteEscaneo,
  moveEscaneo,
} from "@/data/escaneos";
import type {
  EscaneoListado,
  EscaneoResultResponse,
  AuditoriaResponse,
} from "@/data/types";
import EscaneosListado from "./EscaneosListado";

const PAGE_SIZE = 8;

export default function EscaneosListadoPage() {
  const [escaneos, setEscaneos] = useState<EscaneoListado[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [escaneoDetalle, setEscaneoDetalle] =
    useState<EscaneoResultResponse | null>(null);
  const [loadingDetalle, setLoadingDetalle] = useState(false);

  const [auditorias, setAuditorias] = useState<AuditoriaResponse[]>([]);
  const [escaneoMover, setEscaneoMover] = useState<EscaneoListado | null>(null);
  const [nuevaAuditoriaId, setNuevaAuditoriaId] = useState("");
  const [moving, setMoving] = useState(false);

  const cargarDatos = async () => {
    try {
      setLoading(true);
      setError(null);

      const [escaneosResponse, auditoriasResponse] = await Promise.all([
        fetchEscaneos(page, PAGE_SIZE),
        fetchAuditorias(),
      ]);

      setEscaneos(escaneosResponse.content);
      setTotalPages(escaneosResponse.totalPages);
      setTotalElements(escaneosResponse.totalElements);
      setAuditorias(auditoriasResponse);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudieron cargar los datos."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void cargarDatos();
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

  const handleVerDetalles = async (escaneo: EscaneoListado) => {
    try {
      setLoadingDetalle(true);
      const detalle = await fetchEscaneo(
        escaneo.auditoriaId,
        escaneo.escaneoId
      );
      setEscaneoDetalle(detalle);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudieron cargar los detalles."
      );
    } finally {
      setLoadingDetalle(false);
    }
  };

  const handleEliminar = async (escaneo: EscaneoListado) => {
    const confirmar = window.confirm(
      `¿Eliminar el escaneo de la auditoría "${escaneo.auditoriaNombre}"?`
    );
    if (!confirmar) return;

    try {
      await deleteEscaneo(escaneo.auditoriaId, escaneo.escaneoId);
      setEscaneos((actuales) =>
        actuales.filter((item) => item.escaneoId !== escaneo.escaneoId)
      );
      setTotalElements((prev) => Math.max(0, prev - 1));
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudo eliminar el escaneo."
      );
    }
  };

  const openMoverModal = (escaneo: EscaneoListado) => {
    setEscaneoMover(escaneo);
    setNuevaAuditoriaId(escaneo.auditoriaId);
  };

  const handleMover = async () => {
    if (!escaneoMover || !nuevaAuditoriaId) return;

    try {
      setMoving(true);
      setError(null);

      await moveEscaneo(escaneoMover.escaneoId, nuevaAuditoriaId);

      const nuevaAuditoria = auditorias.find(
        (a) => a.id === nuevaAuditoriaId
      );

      if (nuevaAuditoria) {
        setEscaneos((actuales) =>
          actuales.map((escaneo) =>
            escaneo.escaneoId === escaneoMover.escaneoId
              ? {
                  ...escaneo,
                  auditoriaId: nuevaAuditoria.id,
                  auditoriaNombre: nuevaAuditoria.nombre,
                }
              : escaneo
          )
        );
      }

      setEscaneoMover(null);
      setNuevaAuditoriaId("");
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudo mover el escaneo."
      );
    } finally {
      setMoving(false);
    }
  };

  if (loading) {
    return (
      <section className="listado-page">
        <div className="listado-header">
          <div>
            <h1>Escaneos</h1>
            <p>Cargando escaneos...</p>
          </div>
        </div>
        <div className="listado-loading">Cargando...</div>
      </section>
    );
  }

  if (error && escaneos.length === 0) {
    return (
      <section className="listado-page">
        <div className="listado-header">
          <div>
            <h1>Escaneos</h1>
          </div>
        </div>
        <div className="listado-error">
          <p>{error}</p>
          <button
            type="button"
            className="button button-error"
            onClick={() => void cargarDatos()}
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
          <h1>Escaneos</h1>
          <p>Historial de escaneos realizados.</p>
        </div>
        <span className="listado-total">
          {totalElements} {totalElements === 1 ? "escaneo" : "escaneos"}
        </span>
      </div>

      {error && (
        <div className="listado-error" style={{ marginBottom: 16 }}>
          <p>{error}</p>
        </div>
      )}

      <EscaneosListado
        escaneos={escaneos}
        page={page}
        totalPages={totalPages}
        onAnterior={paginaAnterior}
        onSiguiente={siguientePagina}
        onVerDetalles={handleVerDetalles}
        onMover={openMoverModal}
        onEliminar={handleEliminar}
        escaneoDetalle={escaneoDetalle}
        loadingDetalle={loadingDetalle}
        onCerrarDetalle={() => setEscaneoDetalle(null)}
        escaneoMover={escaneoMover}
        auditorias={auditorias}
        nuevaAuditoriaId={nuevaAuditoriaId}
        onChangeAuditoria={setNuevaAuditoriaId}
        onConfirmarMover={handleMover}
        onCancelarMover={() => {
          setEscaneoMover(null);
          setNuevaAuditoriaId("");
        }}
        moving={moving}
      />
    </section>
  );
}