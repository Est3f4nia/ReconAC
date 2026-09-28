import { useEffect, useState } from "react";

import {
  deleteEscaneo,
  fetchEscaneo,
  fetchEscaneos,
  moveEscaneo,
} from "@/data/escaneos";

import { fetchAuditorias } from "@/data/auditorias";

import type {
  AuditoriaResponse,
  EscaneoListado,
  EscaneoResultResponse,
} from "@/data/types";

import EscaneosListado from "./EscaneosListado";

const PAGE_SIZE = 8;

function getEscaneoIds(
  escaneo: EscaneoListado,
): {
  auditoriaId: string;
  escaneoId: string;
} {
  if (!escaneo.auditoriaId || !escaneo.escaneoId) {
    throw new Error(
      "El escaneo no contiene los identificadores necesarios.",
    );
  }

  return {
    auditoriaId: escaneo.auditoriaId,
    escaneoId: escaneo.escaneoId,
  };
}

export default function EscaneosListadoPage() {
  const [escaneos, setEscaneos] =
    useState<EscaneoListado[]>([]);

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] =
    useState<string | null>(null);

  const [escaneoDetalle, setEscaneoDetalle] =
    useState<EscaneoResultResponse | null>(null);

  const [loadingDetalle, setLoadingDetalle] =
    useState(false);

  const [auditorias, setAuditorias] =
    useState<AuditoriaResponse[]>([]);

  const [escaneoMover, setEscaneoMover] =
    useState<EscaneoListado | null>(null);

  const [nuevaAuditoriaId, setNuevaAuditoriaId] =
    useState("");

  const [moving, setMoving] = useState(false);

  const cargarDatos = async () => {
    try {
      setLoading(true);
      setError(null);

      const [
        escaneosResponse,
        auditoriasResponse,
      ] = await Promise.all([
        fetchEscaneos(page, PAGE_SIZE),
        fetchAuditorias(),
      ]);

      setEscaneos(
        escaneosResponse.content ?? [],
      );

      setTotalPages(
        escaneosResponse.totalPages ?? 0,
      );

      setTotalElements(
        escaneosResponse.totalElements ?? 0,
      );

      setAuditorias(auditoriasResponse);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudieron cargar los datos.",
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

  const handleVerDetalles = async (
    escaneo: EscaneoListado,
  ) => {
    try {
      setLoadingDetalle(true);
      setError(null);

      const {
        auditoriaId,
        escaneoId,
      } = getEscaneoIds(escaneo);

      const detalle = await fetchEscaneo(
        auditoriaId,
        escaneoId,
      );

      setEscaneoDetalle(detalle);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudieron cargar los detalles.",
      );
    } finally {
      setLoadingDetalle(false);
    }
  };

  const handleEliminar = async (
    escaneo: EscaneoListado,
  ) => {
    try {
      setError(null);

      const {
        auditoriaId,
        escaneoId,
      } = getEscaneoIds(escaneo);

      await deleteEscaneo(
        auditoriaId,
        escaneoId,
      );

      setEscaneos((actuales) =>
        actuales.filter(
          (item) =>
            item.escaneoId !== escaneoId,
        ),
      );

      setTotalElements((prev) =>
        Math.max(0, prev - 1),
      );
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudo eliminar el escaneo.",
      );
    }
  };

  const openMoverModal = (
    escaneo: EscaneoListado,
  ) => {
    setEscaneoMover(escaneo);
    setNuevaAuditoriaId(
      escaneo.auditoriaId ?? "",
    );
  };

  const handleMover = async () => {
    if (
      !escaneoMover ||
      !escaneoMover.escaneoId ||
      !nuevaAuditoriaId
    ) {
      return;
    }

    try {
      setMoving(true);
      setError(null);

      const escaneoId =
        escaneoMover.escaneoId;

      await moveEscaneo(
        escaneoId,
        nuevaAuditoriaId,
      );

      const nuevaAuditoria =
        auditorias.find(
          (auditoria) =>
            auditoria.id ===
            nuevaAuditoriaId,
        );

      setEscaneos((actuales) =>
        actuales.map((escaneo) =>
          escaneo.escaneoId === escaneoId
            ? {
                ...escaneo,
                auditoriaId:
                  nuevaAuditoria?.id ??
                  nuevaAuditoriaId,
                auditoriaNombre:
                  nuevaAuditoria?.nombre ??
                  escaneo.auditoriaNombre,
              }
            : escaneo,
        ),
      );

      setEscaneoMover(null);
      setNuevaAuditoriaId("");
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudo mover el escaneo.",
      );
    } finally {
      setMoving(false);
    }
  };

  if (loading) {
    return (
      <div className="dashboard-page">
        <section className="listado-page">
          <div className="listado-header">
            <div>
              <h1>Escaneos</h1>
              <p>Cargando escaneos...</p>
            </div>
          </div>

          <div className="listado-loading">
            Cargando...
          </div>
        </section>
      </div>
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
          <p>
            Historial de escaneos realizados.
          </p>
        </div>

        <span className="listado-total">
          {totalElements}{" "}
          {totalElements === 1
            ? "escaneo"
            : "escaneos"}
        </span>
      </div>

      {error && (
        <div
          className="listado-error"
          style={{ marginBottom: 16 }}
        >
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
        onCerrarDetalle={() =>
          setEscaneoDetalle(null)
        }
        escaneoMover={escaneoMover}
        auditorias={auditorias}
        nuevaAuditoriaId={nuevaAuditoriaId}
        onChangeAuditoria={
          setNuevaAuditoriaId
        }
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