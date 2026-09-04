import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  fetchAuditoria,
  fetchEscaneo,
  fetchEscaneoStatus,
  fetchResumen,
  startEscaneo,
} from "@/data/escaneos";
import type {
  AuditoriaResponse,
  EscaneoResultResponse,
  Estado,
  ScanStatusResponse,
} from "@/data/types";
import "@/pages/private/styles/AuditoriaDetail.css";

const POLL_INTERVAL_MS = 3_000;

function esEstadoFinal(estado: Estado) {
  return estado === "COMPLETADO" || estado === "FALLO";
}

function textoEstado(estado: Estado) {
  return estado.replace("_", " ");
}

function objetivosDesdeTexto(texto: string) {
  return [
    ...new Set(
      texto
        .split(/[\n,]/)
        .map((valor) => valor.trim())
        .filter(Boolean),
    ),
  ];
}

export default function AuditoriaDetailPage() {
  const { auditoriaId, escaneoId: escaneoIdRuta } = useParams<{
    auditoriaId: string;
    escaneoId?: string;
  }>();

  const [auditoria, setAuditoria] = useState<AuditoriaResponse | null>(null);
  const [escaneoId, setEscaneoId] = useState<string | null>(
    escaneoIdRuta ?? null,
  );
  const [estado, setEstado] = useState<ScanStatusResponse | null>(null);
  const [resultado, setResultado] = useState<EscaneoResultResponse | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [errorEscaneo, setErrorEscaneo] = useState("");
  const [objetivos, setObjetivos] = useState("");
  const [nvdApiKey, setNvdApiKey] = useState("");
  const [iniciando, setIniciando] = useState(false);

  useEffect(() => {
    if (!auditoriaId) {
      setError("Parámetros inválidos");
      setCargando(false);
      return;
    }

    const auditoriaIdValido = auditoriaId;
    let vigente = true;

    async function cargarContexto() {
      try {
        setCargando(true);
        setError("");

        const [auditoriaData, resumen] = await Promise.all([
          fetchAuditoria(auditoriaIdValido),
          fetchResumen(),
        ]);

        if (!vigente) return;

        const ultimoEscaneo = resumen.find(
          (item) => item.auditoriaId === auditoriaIdValido,
        );

        setAuditoria(auditoriaData);
        setEscaneoId(escaneoIdRuta ?? ultimoEscaneo?.escaneoId ?? null);
        setEstado(
          ultimoEscaneo?.status
            ? {
                scanId: null,
                status: ultimoEscaneo.status,
                progress: 0,
                error: null,
              }
            : null,
        );
      } catch (err) {
        if (vigente) {
          setError(
            err instanceof Error ? err.message : "Error al cargar la auditoría",
          );
        }
      } finally {
        if (vigente) setCargando(false);
      }
    }

    cargarContexto();

    return () => {
      vigente = false;
    };
  }, [auditoriaId, escaneoIdRuta]);

  useEffect(() => {
    if (!auditoriaId || !escaneoId) return;

    const auditoriaIdValido = auditoriaId;
    const escaneoIdValido = escaneoId;
    let vigente = true;
    let proximaActualizacion: number | undefined;

    async function actualizarEstado() {
      let estadoActual: ScanStatusResponse | null = null;

      try {
        estadoActual = await fetchEscaneoStatus(auditoriaIdValido, escaneoIdValido);
        if (!vigente) return;

        setEstado(estadoActual);
        setErrorEscaneo("");

        if (esEstadoFinal(estadoActual.status)) {
          try {
            const detalle = await fetchEscaneo(auditoriaIdValido, escaneoIdValido);
            if (vigente) setResultado(detalle);
          } catch (err) {
            if (vigente) {
              setErrorEscaneo(
                err instanceof Error
                  ? err.message
                  : "No se pudo obtener el resultado del escaneo",
              );
            }
          }
        }
      } catch (err) {
        if (vigente) {
          setErrorEscaneo(
            err instanceof Error
              ? err.message
              : "No se pudo actualizar el estado del escaneo",
          );
        }
      } finally {
        if (vigente && (!estadoActual || !esEstadoFinal(estadoActual.status))) {
          proximaActualizacion = window.setTimeout(
            actualizarEstado,
            POLL_INTERVAL_MS,
          );
        }
      }
    }

    actualizarEstado();

    return () => {
      vigente = false;
      if (proximaActualizacion) window.clearTimeout(proximaActualizacion);
    };
  }, [auditoriaId, escaneoId]);

  async function iniciarEscaneo(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!auditoriaId) return;

    const destinos = objetivosDesdeTexto(objetivos);
    if (destinos.length === 0) {
      setErrorEscaneo("Indicá al menos un dominio, IP o rango de red.");
      return;
    }

    try {
      setIniciando(true);
      setErrorEscaneo("");
      setResultado(null);

      const creado = await startEscaneo(auditoriaId, {
        objetivos: destinos,
        nvdApiKey: nvdApiKey.trim() || undefined,
      });

      // El POST no incluye el identificador. El resumen devuelve el último
      // escaneo de esta auditoría inmediatamente después de su creación.
      const resumen = await fetchResumen();
      const auditoriaResumen = resumen.find(
        (item) => item.auditoriaId === auditoriaId,
      );

      if (!auditoriaResumen?.escaneoId) {
        throw new Error(
          "El escaneo fue creado, pero no se pudo recuperar su identificador.",
        );
      }

      setEstado({
        scanId: null,
        status: creado.estado,
        progress: creado.progreso,
        error: creado.mensajeError,
      });
      setEscaneoId(auditoriaResumen.escaneoId);
      setObjetivos("");
      setNvdApiKey("");
    } catch (err) {
      setErrorEscaneo(
        err instanceof Error ? err.message : "No se pudo iniciar el escaneo",
      );
    } finally {
      setIniciando(false);
    }
  }

  if (cargando) {
    return <p className="auditoria-detail-feedback">Cargando...</p>;
  }

  if (error) {
    return <p className="auditoria-detail-feedback">{error}</p>;
  }

  if (!auditoria) {
    return <p className="auditoria-detail-feedback">No se encontró la auditoría.</p>;
  }

  const escaneoEnCurso = estado && !esEstadoFinal(estado.status);
  const hosts = resultado?.resultado?.hosts ?? [];

  return (
    <main className="auditoria-detail-page">
      <Link className="auditoria-detail-back" to="/dashboard">
        ← Volver a auditorías
      </Link>

      <header className="auditoria-detail-header">
        <p className="auditoria-detail-eyebrow">Auditoría</p>
        <h1>{auditoria.nombre}</h1>
        <p>{auditoria.objetivo}</p>
      </header>

      <section className="scan-panel" aria-labelledby="iniciar-escaneo-title">
        <div className="scan-panel-heading">
          <div>
            <h2 id="iniciar-escaneo-title">Iniciar escaneo</h2>
            <p>Ingresá un objetivo por línea; también podés separarlos con comas.</p>
          </div>
          {escaneoEnCurso && <span className="scan-live-label">Escaneo en curso</span>}
        </div>

        <form className="scan-form" onSubmit={iniciarEscaneo}>
          <label htmlFor="objetivos">Objetivos</label>
          <textarea
            id="objetivos"
            value={objetivos}
            onChange={(event) => setObjetivos(event.target.value)}
            placeholder={"192.168.1.10\nexample.com"}
            disabled={iniciando || Boolean(escaneoEnCurso)}
            required
          />

          <label htmlFor="nvd-api-key">
            Clave de API NVD
          </label>
          <input
            id="nvd-api-key"
            type="password"
            autoComplete="off"
            aria-describedby="nvd-api-key-help"
            value={nvdApiKey}
            onChange={(event) => setNvdApiKey(event.target.value)}
            disabled={iniciando || Boolean(escaneoEnCurso)}
          />
          <p id="nvd-api-key-help" className="scan-field-note">
            Si no ingresás una clave, el reconocimiento se ejecutará igual, pero
            no se enriquecerán los hallazgos con consultas a APIs externas.
          </p>

          <button
            className="button scan-submit"
            type="submit"
            disabled={iniciando || Boolean(escaneoEnCurso)}
          >
            {iniciando ? "Iniciando..." : "Iniciar escaneo"}
          </button>
        </form>
      </section>

      <section
        className="scan-panel"
        aria-live="polite"
        aria-labelledby="estado-escaneo-title"
      >
        <div className="scan-panel-heading">
          <div>
            <h2 id="estado-escaneo-title">Estado del escaneo</h2>
            <p>
              {escaneoId
                ? "Se actualiza automáticamente cada 3 segundos."
                : "Todavía no hay escaneos para esta auditoría."}
            </p>
          </div>
          {estado && (
            <span className={`status status-${estado.status}`}>
              {textoEstado(estado.status)}
            </span>
          )}
        </div>

        {estado && (
          <div className="scan-progress">
            <div className="scan-progress-values">
              <span>Progreso</span>
              <strong>{estado.progress}%</strong>
            </div>
            <progress value={estado.progress} max="100">
              {estado.progress}%
            </progress>
            {estado.error && <p className="scan-error">{estado.error}</p>}
          </div>
        )}

        {errorEscaneo && <p className="scan-error">{errorEscaneo}</p>}
      </section>

      {estado?.status === "COMPLETADO" && (
        <section className="scan-panel" aria-labelledby="resultado-escaneo-title">
          <h2 id="resultado-escaneo-title">Resultados</h2>
          {hosts.length === 0 ? (
            <p>No hay activos disponibles para mostrar.</p>
          ) : (
            <ul className="scan-host-list">
              {hosts.map((host) => (
                <li key={`${host.ip}-${host.mac ?? "sin-mac"}`}>
                  <strong>{host.ip}</strong>
                  <span>{host.hostname ?? "Sin hostname"}</span>
                  <span>{host.os ?? "Sistema operativo no detectado"}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </main>
  );
}
