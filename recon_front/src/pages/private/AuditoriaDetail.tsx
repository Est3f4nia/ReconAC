import { useCallback, useEffect, useRef, useState } from "react";
import { fetchAuditoria, fetchEscaneoStatus, startEscaneo } from "@/data/escaneos";
import { fetchDashboardAuditoria } from "@/data/auditorias";
import { AuditoriaHeader } from "@/components/metricas/AuditoriaHeader";
import { ScanForm } from "@/components/metricas/ScanForm";
import { DashboardKpiGrid } from "@/components/metricas/DashboardKpiGrid";
import { ScanHistory } from "@/components/metricas/ScanHistory";
import { RiskTimeline } from "@/components/metricas/RiskTimeline";
import { CveBreakdown } from "@/components/metricas/CveBreakdown";
import { HostBreakdown } from "@/components/metricas/HostBreakdown";
import { ScanComparison } from "@/components/metricas/ScanComparison";
import { ReportGenerator } from "@/components/metricas/ReportGenerator";
import type { AuditoriaResponse, DashboardAuditoria, ScanStartRequest, ScanStatusResponse } from "@/data/types";
import "@/pages/private/styles/AuditoriaDetail.css";

const esEscaneoActivo = (scan: ScanStatusResponse) => ["PENDIENTE", "EN_PROCESO"].includes(scan.status);

export default function AuditoriaDetail({ id }: { id: string }) {
  const [auditoria, setAuditoria] = useState<AuditoriaResponse | null>(null);
  const [dashboard, setDashboard] = useState<DashboardAuditoria | null>(null);
  const [estadosEscaneos, setEstadosEscaneos] = useState<Record<string, ScanStatusResponse>>({});
  const estadosRef = useRef(estadosEscaneos);
  const alive = useRef(true);
  const requestVersion = useRef(0);
  const [cargando, setCargando] = useState(true);
  const [iniciandoEscaneo, setIniciandoEscaneo] = useState(false);
  const [preferredScanId, setPreferredScanId] = useState<string>();
  const [error, setError] = useState("");

  const actualizarEstados = useCallback((next: Record<string, ScanStatusResponse>) => {
    estadosRef.current = next;
    setEstadosEscaneos(next);
  }, []);

  const cargarDatos = useCallback(async () => {
    const version = ++requestVersion.current;
    try {
      const [audit, data] = await Promise.all([fetchAuditoria(id), fetchDashboardAuditoria(id)]);
      if (!alive.current || version !== requestVersion.current) return;
      setAuditoria(audit);
      setDashboard(data);
      setError("");
      const next = { ...estadosRef.current };
      for (const scan of data.historial) {
        const saved: ScanStatusResponse = { scanId: scan.escaneoId, status: scan.estado,
          progress: scan.progreso, error: scan.mensajeError ?? null };
        const live = next[scan.escaneoId];
        next[scan.escaneoId] = live && !esEscaneoActivo(live) ? live
          : !esEscaneoActivo(saved) ? saved : live ?? saved;
      }
      actualizarEstados(next);
    } catch (err) {
      if (alive.current && version === requestVersion.current)
        setError(err instanceof Error ? err.message : "No se pudo actualizar la auditoría.");
    } finally {
      if (alive.current && version === requestVersion.current) setCargando(false);
    }
  }, [id, actualizarEstados]);

  useEffect(() => {
    alive.current = true;
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;
    let ticks = 0;
    void cargarDatos();
    async function poll() {
      try {
        const active = Object.values(estadosRef.current).filter(esEscaneoActivo);
        const results = await Promise.allSettled(active.map(scan => fetchEscaneoStatus(id, scan.scanId!)));
        if (cancelled) return;
        // Mezclar al finalizar la consulta: puede haberse creado otro escaneo mientras tanto.
        const next = { ...estadosRef.current };
        let finished = false;
        for (const result of results) {
          if (result.status === "fulfilled" && result.value.scanId) {
            next[result.value.scanId] = result.value;
            finished ||= !esEscaneoActivo(result.value);
          }
        }
        actualizarEstados(next);
        if (results.some(result => result.status === "rejected"))
          setError("No se pudo actualizar algún escaneo. Reintentando la conexión…");
        if (finished || ++ticks % 5 === 0) await cargarDatos();
      } finally {
        if (!cancelled) timer = setTimeout(poll, 3000);
      }
    }
    timer = setTimeout(poll, 3000);
    return () => {
      cancelled = true;
      alive.current = false;
      requestVersion.current++;
      clearTimeout(timer);
    };
  }, [id, cargarDatos, actualizarEstados]);

  async function handleStartScan(request: ScanStartRequest) {
    if (iniciandoEscaneo) return;
    setIniciandoEscaneo(true);
    setError("");
    try {
      const scan = await startEscaneo(id, request);
      if (!alive.current) return;
      if (!scan.escaneoId) throw new Error("El backend no devolvió el identificador del escaneo.");
      actualizarEstados({ ...estadosRef.current, [scan.escaneoId]: {
        scanId: scan.escaneoId, status: scan.estado, progress: scan.progreso, error: scan.mensajeError,
      }});
      setPreferredScanId(scan.escaneoId);
      await cargarDatos();
    } catch (err) {
      if (alive.current) setError(err instanceof Error ? err.message : "No se pudo iniciar el escaneo.");
    } finally { if (alive.current) setIniciandoEscaneo(false); }
  }

  const escaneos = (dashboard?.historial ?? []).map(scan => ({
    ...scan, estado: estadosEscaneos[scan.escaneoId]?.status ?? scan.estado,
    progreso: estadosEscaneos[scan.escaneoId]?.progress ?? scan.progreso,
    mensajeError: estadosEscaneos[scan.escaneoId]?.error ?? scan.mensajeError,
  }));
  const escaneosActivos = Object.values(estadosEscaneos).filter(esEscaneoActivo);
  if (cargando) return <main className="dashboard-page auditoria-detail"><div className="dashboard-empty" role="status">Cargando auditoría…</div></main>;
  if (!auditoria) return <main className="dashboard-page auditoria-detail"><div className="dashboard-error" role="alert">
    {error || "Auditoría no encontrada."}
    <button className="button button-page" onClick={() => void cargarDatos()}>Reintentar</button>
  </div></main>;
  return (
    <main className="dashboard-page auditoria-detail">
      {/* =====================================================
          1. HEADER
          ===================================================== */}

      <AuditoriaHeader
        auditoria={auditoria}
      />

      {error && (
        <div
          className="dashboard-error"
          role="alert"
        >
          {error}
        </div>
      )}

      {/* =====================================================
          2. NUEVO ESCANEO
          ===================================================== */}

      <section className="auditoria-section scan-form-section">
        <ScanForm
          onSubmit={handleStartScan}
          loading={iniciandoEscaneo}
          auditoriaId={id}
          scans={Object.values(estadosEscaneos)}
          preferredScanId={preferredScanId}
        />
      </section>

      {/* =====================================================
          4. ESCANEOS EN PROGRESO
          ===================================================== */}

      {escaneosActivos.length > 0 && (
        <section
          className="auditoria-section dashboard-section"
          aria-labelledby="active-scans-title"
        >
          <div className="dashboard-section-heading">
            <p className="dashboard-section-eyebrow">
              Ejecución
            </p>

            <h2 id="active-scans-title">
              Escaneos en progreso
            </h2>

            <p>
              ReconAC está procesando
              las ejecuciones
              actualmente activas.
            </p>
          </div>

          <div className="scan-progress-list">
            {escaneosActivos.map(
              (estado) => {
                const progreso =
                  Math.min(
                    100,
                    Math.max(
                      0,
                      estado.progress ??
                        0,
                    ),
                  );

                return (
                  <div
                    className="scan-progress"
                    key={
                      estado.scanId ??
                      "scan-progress"
                    }
                  >
                    <div className="scan-progress-header">
                      <span>
                        Escaneo{" "}
                        {estado.scanId
                          ? estado.scanId.slice(
                              0,
                              8,
                            )
                          : "—"}
                      </span>

                      <span>
                        {progreso}%
                      </span>
                    </div>

                    <div
                      className="scan-progress-bar"
                      role="progressbar"
                      aria-valuenow={
                        progreso
                      }
                      aria-valuemin={
                        0
                      }
                      aria-valuemax={
                        100
                      }
                    >
                      <div
                        className="scan-progress-value"
                        style={{
                          width: `${progreso}%`,
                        }}
                      />
                    </div>

                    <div className="scan-progress-status">
                      {estado.status ===
                      "PENDIENTE"
                        ? "Pendiente de ejecución"
                        : "Procesando"}
                    </div>
                  </div>
                );
              },
            )}
          </div>
        </section>
      )}

      {/* =====================================================
          5. KPIs
          ===================================================== */}

      {dashboard && (
        <section className="auditoria-section">
          <DashboardKpiGrid
            data={dashboard.kpis}
          />
        </section>
      )}

      {/* =====================================================
          6. TIMELINE DE RIESGO
          ===================================================== */}

      {dashboard && (
        <section className="auditoria-section risk-section">
          <RiskTimeline
            data={
              dashboard.riesgoTemporal
            }
          />
        </section>
      )}

      {/* =====================================================
          7. HOSTS + CVEs
          ===================================================== */}

      {dashboard && (
        <section className="auditoria-section breakdown-row">
          <div className="breakdown-col">
            <HostBreakdown
              data={dashboard.hosts}
              pageSize={5}
            />
          </div>

          <div className="breakdown-col">
            <CveBreakdown
              data={
                dashboard.vulnerabilidades
              }
              pageSize={5}
            />
          </div>
        </section>
      )}

      {/* =====================================================
          8. COMPARACIÓN
          ===================================================== */}

      {dashboard && (
        <section className="auditoria-section">
          <ScanComparison
            auditoriaId={id}
            escaneos={escaneos}
          />
        </section>
      )}

      {/* =====================================================
          9. HISTORIAL
          ===================================================== */}

      {dashboard && (
        <section className="auditoria-section">
          <ScanHistory
            auditoriaId={id}
            escaneos={escaneos}
            pageSize={6}
          />
        </section>
      )}

      {/* =====================================================
          10. REPORTES
          ===================================================== */}

      {dashboard && (
        <section className="auditoria-section report-section">
          <ReportGenerator
            auditoriaId={id}
            escaneos={escaneos}
          />
        </section>
      )}
    </main>
  );
}


