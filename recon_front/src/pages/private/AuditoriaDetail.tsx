import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import {
  fetchAuditoria,
  fetchEscaneoStatus,
  fetchEscaneos,
  startEscaneo,
} from "@/data/escaneos";

import { fetchDashboardAuditoria } from "@/data/auditorias";

import type {
  AuditoriaResponse,
  DashboardAuditoria,
  Estado,
  EscaneoListado,
  ScanStartRequest,
  ScanStatusResponse,
} from "@/data/types";

import { AuditoriaHeader } from "@/components/metricas/AuditoriaHeader";
import { ScanForm } from "@/components/metricas/ScanForm";
import { DashboardKpiGrid } from "@/components/metricas/DashboardKpiGrid";
import { ScanHistory } from "@/components/metricas/ScanHistory";
import { RiskTimeline } from "@/components/metricas/RiskTimeline";
import { CveBreakdown } from "@/components/metricas/CveBreakdown";
import { HostBreakdown } from "@/components/metricas/HostBreakdown";
import { ScanComparison } from "@/components/metricas/ScanComparison";
import { ReportGenerator } from "@/components/metricas/ReportGenerator";

import "@/pages/private/styles/AuditoriaDetail.css";

const POLL_INTERVAL_MS = 3_000;

function esEstadoFinal(estado: Estado) {
  return estado === "COMPLETADO" || estado === "FALLO";
}

function ordenarPorCreadoDesc(
  escaneos: EscaneoListado[],
): EscaneoListado[] {
  return [...escaneos].sort(
    (a, b) =>
      new Date(b.creadoA).getTime() -
      new Date(a.creadoA).getTime(),
  );
}

export default function AuditoriaDetailPage() {
  const { auditoriaId } = useParams<{
    auditoriaId: string;
    escaneoId?: string;
  }>();

  const [auditoria, setAuditoria] =
    useState<AuditoriaResponse | null>(null);

  const [dashboard, setDashboard] =
    useState<DashboardAuditoria | null>(null);

  const [escaneoActivo, setEscaneoActivo] =
    useState<string | null>(null);

  const [estado, setEstado] =
    useState<ScanStatusResponse | null>(null);

  const [cargando, setCargando] = useState(true);
  const [cargandoDashboard, setCargandoDashboard] = useState(false);
  const [iniciandoEscaneo, setIniciandoEscaneo] = useState(false);

  const [error, setError] = useState("");
  const [errorEscaneo, setErrorEscaneo] = useState("");

  const cargarDashboard = useCallback(async () => {
    if (!auditoriaId) {
      return;
    }

    const id = auditoriaId;

    try {
      setCargandoDashboard(true);

      const data = await fetchDashboardAuditoria(id);

      setDashboard(data);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudo cargar el dashboard.",
      );
    } finally {
      setCargandoDashboard(false);
    }
  }, [auditoriaId]);

  const cargarEscaneoActivo = useCallback(async () => {
    if (!auditoriaId) {
      return;
    }

    const id = auditoriaId;

    const response = await fetchEscaneos(0, 100);

    const deEstaAuditoria =
      response.content.filter(
        (escaneo) => escaneo.auditoriaId === id,
      );

    const ordenados =
      ordenarPorCreadoDesc(deEstaAuditoria);

    const ultimo = ordenados[0];

    if (!ultimo) {
      setEscaneoActivo(null);
      setEstado(null);
      return;
    }

    setEscaneoActivo(ultimo.escaneoId);

    setEstado({
      scanId: ultimo.escaneoId,
      status: ultimo.estado,
      progress: ultimo.progreso,
      error: null,
    });
  }, [auditoriaId]);

  useEffect(() => {
    if (!auditoriaId) {
      setError("No se indicó una auditoría.");
      setCargando(false);
      return;
    }

    const id = auditoriaId;
    let cancelled = false;

    async function load() {
      try {
        setCargando(true);
        setError("");

        const [
          auditoriaData,
          escaneosResponse,
          dashboardData,
        ] = await Promise.all([
          fetchAuditoria(id),
          fetchEscaneos(0, 100),
          fetchDashboardAuditoria(id),
        ]);

        if (cancelled) {
          return;
        }

        setAuditoria(auditoriaData);
        setDashboard(dashboardData);

        const deEstaAuditoria =
          escaneosResponse.content.filter(
            (escaneo) =>
              escaneo.auditoriaId === id,
          );

        const ordenados =
          ordenarPorCreadoDesc(deEstaAuditoria);

        const ultimo = ordenados[0];

        if (ultimo) {
          setEscaneoActivo(ultimo.escaneoId);

          setEstado({
            scanId: ultimo.escaneoId,
            status: ultimo.estado,
            progress: ultimo.progreso,
            error: null,
          });
        } else {
          setEscaneoActivo(null);
          setEstado(null);
        }
      } catch (err) {
        if (cancelled) {
          return;
        }

        setError(
          err instanceof Error
            ? err.message
            : "No se pudo cargar la auditoría.",
        );
      } finally {
        if (!cancelled) {
          setCargando(false);
        }
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, [auditoriaId]);

  useEffect(() => {
    if (!auditoriaId || !escaneoActivo) {
      return;
    }

    const id = auditoriaId;
    const scanId = escaneoActivo;

    let cancelled = false;
    let timeoutId: ReturnType<typeof setTimeout> | null = null;

    async function poll() {
      try {
        const status =
          await fetchEscaneoStatus(id, scanId);

        if (cancelled) {
          return;
        }

        setEstado(status);

        if (esEstadoFinal(status.status)) {
          if (status.status === "FALLO") {
            setErrorEscaneo(
              status.error ??
                "La ejecución terminó con errores.",
            );
          } else {
            setErrorEscaneo("");
          }

          await cargarDashboard();

          if (!cancelled) {
            setEscaneoActivo(null);
          }

          return;
        }

        timeoutId = setTimeout(
          poll,
          POLL_INTERVAL_MS,
        );
      } catch (err) {
        if (cancelled) {
          return;
        }

        setErrorEscaneo(
          err instanceof Error
            ? err.message
            : "No se pudo consultar el estado del escaneo.",
        );

        timeoutId = setTimeout(
          poll,
          POLL_INTERVAL_MS,
        );
      }
    }

    poll();

    return () => {
      cancelled = true;

      if (timeoutId !== null) {
        clearTimeout(timeoutId);
      }
    };
  }, [
    auditoriaId,
    escaneoActivo,
    cargarDashboard,
  ]);

  async function handleStartScan(
    request: ScanStartRequest,
  ) {
    if (!auditoriaId) {
      return;
    }

    const id = auditoriaId;

    try {
      setIniciandoEscaneo(true);
      setErrorEscaneo("");

      await startEscaneo(id, request);

      const response =
        await fetchEscaneos(0, 100);

      const deEstaAuditoria =
        response.content.filter(
          (escaneo) =>
            escaneo.auditoriaId === id,
        );

      const ordenados =
        ordenarPorCreadoDesc(deEstaAuditoria);

      const nuevoEscaneo = ordenados[0];

      if (!nuevoEscaneo) {
        throw new Error(
          "El escaneo fue iniciado pero no se pudo localizar su ejecución.",
        );
      }

      setEscaneoActivo(
        nuevoEscaneo.escaneoId,
      );

      setEstado({
        scanId: nuevoEscaneo.escaneoId,
        status: nuevoEscaneo.estado,
        progress: nuevoEscaneo.progreso,
        error: null,
      });

      await cargarDashboard();
    } catch (err) {
      setErrorEscaneo(
        err instanceof Error
          ? err.message
          : "No se pudo iniciar el escaneo.",
      );
    } finally {
      setIniciandoEscaneo(false);
    }
  }

  if (cargando) {
    return (
      <main className="dashboard-page auditoria-detail">
        <section className="dashboard-section">
          <p>Cargando auditoría...</p>
        </section>
      </main>
    );
  }

  if (error && !auditoria) {
    return (
      <main className="dashboard-page auditoria-detail">
        <section className="dashboard-section">
          <div className="dashboard-card">
            <h2>Error</h2>
            <p>{error}</p>

            <Link
              to="/dashboard"
              className="button"
            >
              Volver al dashboard
            </Link>
          </div>
        </section>
      </main>
    );
  }

  if (!auditoria) {
    return (
      <main className="dashboard-page auditoria-detail">
        <section className="dashboard-section">
          <p>No se encontró la auditoría.</p>
        </section>
      </main>
    );
  }

  return (
    <main className="dashboard-page auditoria-detail">
      <AuditoriaHeader auditoria={auditoria} />

      <section className="dashboard-section">
        <ScanForm
          onSubmit={handleStartScan}
          loading={iniciandoEscaneo}
        />

        {errorEscaneo && (
          <div className="dashboard-error">
            {errorEscaneo}
          </div>
        )}

        {estado &&
          !esEstadoFinal(estado.status) && (
            <div className="scan-progress">
              <div className="scan-progress-header">
                <span>
                  Estado:{" "}
                  <strong>{estado.status}</strong>
                </span>

                <span>
                  {estado.progress}%
                </span>
              </div>

              <div className="scan-progress-bar">
                <div
                  className="scan-progress-value"
                  style={{
                    width: `${Math.max(
                      0,
                      Math.min(
                        100,
                        estado.progress,
                      ),
                    )}%`,
                  }}
                />
              </div>
            </div>
          )}
      </section>

      {error && (
        <section className="dashboard-section">
          <div className="dashboard-error">
            {error}
          </div>
        </section>
      )}

      {dashboard && (
        <>
          <section className="dashboard-section">
            <DashboardKpiGrid
              data={dashboard.kpis}
            />
          </section>

          <section className="dashboard-section">
            <ScanHistory
              escaneos={dashboard.historial}
            />
          </section>

          <section className="dashboard-section">
            <RiskTimeline
              data={dashboard.riesgoTemporal}
            />
          </section>

          <section className="dashboard-section">
            <CveBreakdown
              data={dashboard.vulnerabilidades}
            />
          </section>

          <section className="dashboard-section">
            <HostBreakdown
              data={dashboard.hosts}
            />
          </section>

          <section className="dashboard-section">
            <ScanComparison
              escaneos={dashboard.historial}
            />
          </section>

          <section className="dashboard-section">
            <ReportGenerator
              auditoriaId={auditoria.id}
            />
          </section>
        </>
      )}

      {cargandoDashboard && (
        <section className="dashboard-section">
          <p>Actualizando métricas...</p>
        </section>
      )}
    </main>
  );
}