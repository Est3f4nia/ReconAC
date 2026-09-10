import { useEffect, useState } from "react";
import { fetchEscaneo } from "@/data/escaneos";
import type {
  EjecucionHistorial,
  EscaneoResultResponse,
} from "@/data/types";

interface Props {
  auditoriaId: string;
  escaneos: EjecucionHistorial[];
  pageSize?: number;
}

export function ScanHistory({
  auditoriaId,
  escaneos,
  pageSize = 6,
}: Props) {
  const [page, setPage] = useState(0);
  const [abierto, setAbierto] = useState<string | null>(null);
  const [resultados, setResultados] = useState<
    Record<string, EscaneoResultResponse>
  >({});
  const [cargando, setCargando] = useState<string | null>(null);
  const [errores, setErrores] = useState<Record<string, string>>({});

  const totalPages = Math.max(1, Math.ceil(escaneos.length / pageSize));
  const safePage = Math.min(page, totalPages - 1);
  const start = safePage * pageSize;
  const visibles = escaneos.slice(start, start + pageSize);

  const selectedScan = escaneos.find(scan => scan.escaneoId === abierto);
  useEffect(() => {
    if (!abierto || selectedScan?.estado !== "COMPLETADO") return;
    let cancelled = false;
    setCargando(abierto);
    setErrores(previous => ({ ...previous, [abierto]: "" }));
    void fetchEscaneo(auditoriaId, abierto).then(result => {
      if (!cancelled) setResultados(previous => ({ ...previous, [abierto]: result }));
    }).catch(error => {
      if (!cancelled) setErrores(previous => ({ ...previous, [abierto]: error instanceof Error ? error.message : "No se pudo cargar el resultado." }));
    }).finally(() => { if (!cancelled) setCargando(null); });
    return () => { cancelled = true; };
  }, [abierto, selectedScan?.estado, auditoriaId]);

  function toggleResultado(escaneoId: string) {
    setAbierto(previous => previous === escaneoId ? null : escaneoId);
  }
  return (
    <section
      className="dashboard-section"
      aria-labelledby="scan-history-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">Trazabilidad</p>
        <h2 id="scan-history-title">Historial de escaneos</h2>
        <p>
          Ejecuciones recientes y resultados obtenidos por ReconAC.
        </p>
      </div>

      <div className="scan-history">
        {escaneos.length === 0 ? (
          <div className="dashboard-empty">
            <p>Todavía no hay ejecuciones registradas.</p>
          </div>
        ) : (
          <>
            {visibles.map((escaneo) => {
              const resultado = resultados[escaneo.escaneoId];
              const error = errores[escaneo.escaneoId];
              const estaAbierto = abierto === escaneo.escaneoId;

              return (
                <article
                  className="scan-history-item"
                  key={escaneo.escaneoId}
                >
                  <button
                    type="button"
                    className="scan-history-toggle"
                    onClick={() =>
                      toggleResultado(escaneo.escaneoId)
                    }
                  >
                    <div>
                      <strong>
                        {new Date(escaneo.fecha).toLocaleString()}
                      </strong>
                      <span>{escaneo.escaneoId}</span>
                    </div>

                    <span
                      className={`status status-${escaneo.estado}`}
                    >
                      {escaneo.estado.replace("_", " ")}
                    </span>

                    <div className="scan-history-summary">
                      <span>Activos: {escaneo.activos}</span>
                      <span>CVEs: {escaneo.cves}</span>
                      <span>
                        Críticos: {escaneo.cvesCriticos}
                      </span>
                    </div>

                    <span className="scan-history-open">
                      {estaAbierto ? "Cerrar" : "Ver resultado"}
                    </span>
                  </button>

                  {estaAbierto && (
                    <div className="console">
                      {escaneo.estado !== "COMPLETADO" && !escaneo.mensajeError && <p>El resultado estará disponible cuando finalice el escaneo.</p>}
                      {escaneo.mensajeError && <div className="console-error" role="alert">{escaneo.mensajeError}</div>}
                      {cargando === escaneo.escaneoId && (
                        <div className="console-line">
                          <span>&gt; Cargando resultado...</span>
                        </div>
                      )}

                      {error && (
                        <div className="console-line console-error">
                          <span>&gt; ERROR: {error}</span>
                        </div>
                      )}

                      {resultado && (
                        <>
                          <div className="console-line console-muted">
                            <span>&gt; RECONAC RESULT</span>
                          </div>

                          <div className="console-line">
                            <span>
                              &gt; Estado: {resultado.estado}
                            </span>
                          </div>

                          <div className="console-line">
                            <span>
                              &gt; Nmap:{" "}
                              {resultado.nmapVersion ??
                                "desconocido"}
                            </span>
                          </div>

                          <div className="console-line">
                            <span>
                              &gt; Progreso: {resultado.progreso}%
                            </span>
                          </div>

                          <div className="console-line">
                            <span>
                              &gt; Hosts detectados:{" "}
                              {resultado.activos.length}
                            </span>
                          </div>

                          {resultado.activos.map((activo) => (
                            <div
                              key={activo.activoId}
                              className="console-line"
                            >
                              <span>
                                &nbsp;&nbsp;├─ {activo.host}
                                {activo.hostname
                                  ? ` (${activo.hostname})`
                                  : ""}
                                {activo.so
                                  ? ` — ${activo.so}`
                                  : ""}
                              </span>

                              <div className="console-line">
                                <span>
                                  &nbsp;&nbsp;&nbsp;&nbsp;├─
                                  Puertos: {activo.puertos.length}
                                </span>
                              </div>

                              {activo.puertos.map((puerto) => (
                                <div
                                  key={`${activo.activoId}-${puerto.numero}-${puerto.protocolo}`}
                                  className="console-line"
                                >
                                  <span>
                                    &nbsp;&nbsp;&nbsp;&nbsp;│
                                    └─ {puerto.numero}/
                                    {puerto.protocolo} [
                                    {puerto.estado}]
                                    {puerto.servicio
                                      ? ` — ${puerto.servicio}`
                                      : ""}
                                  </span>
                                </div>
                              ))}
                            </div>
                          ))}

                          <div className="console-line console-muted">
                            <span>
                              &gt; Inicio:{" "}
                              {resultado.iniciadoA
                                ? new Date(
                                    resultado.iniciadoA
                                  ).toLocaleString()
                                : "—"}
                            </span>
                          </div>

                          <div className="console-line console-muted">
                            <span>
                              &gt; Fin:{" "}
                              {resultado.completadoA
                                ? new Date(
                                    resultado.completadoA
                                  ).toLocaleString()
                                : "—"}
                            </span>
                          </div>
                        </>
                      )}
                    </div>
                  )}
                </article>
              );
            })}

            {escaneos.length > pageSize && (
              <div className="listado-pagination">
                <button
                  type="button"
                  className="button"
                  onClick={() =>
                    setPage((p) => Math.max(0, p - 1))
                  }
                  disabled={safePage === 0}
                >
                  Anterior
                </button>
                <span>
                  Página {safePage + 1} de {totalPages}
                </span>
                <button
                  type="button"
                  className="button"
                  onClick={() =>
                    setPage((p) =>
                      Math.min(totalPages - 1, p + 1)
                    )
                  }
                  disabled={safePage >= totalPages - 1}
                >
                  Siguiente
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </section>
  );
}

