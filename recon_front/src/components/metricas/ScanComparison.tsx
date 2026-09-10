import { useEffect, useMemo, useState } from "react";
import { fetchEscaneo } from "@/data/escaneos";
import type {
  EjecucionHistorial,
  EscaneoResultResponse,
  ActivoResultadoResponse,
  PuertoResultadoResponse,
} from "@/data/types";

interface Props {
  auditoriaId: string;
  escaneos: EjecucionHistorial[];
}

interface HostComparison {
  host: string;
  hostnameA: string | null;
  hostnameB: string | null;
  nuevo: boolean;
  eliminado: boolean;
  puertosNuevos: PuertoResultadoResponse[];
  puertosEliminados: PuertoResultadoResponse[];
  cpesNuevos: string[];
  cpesEliminados: string[];
}

function clavePuerto(puerto: PuertoResultadoResponse) {
  return `${puerto.protocolo}/${puerto.numero}`;
}

function mapaPuertos(activo: ActivoResultadoResponse | undefined) {
  return new Map(
    (activo?.puertos ?? []).map((puerto) => [
      clavePuerto(puerto),
      puerto,
    ]),
  );
}

function mapaActivos(resultado: EscaneoResultResponse | null) {
  return new Map(
    (resultado?.activos ?? []).map((activo) => [activo.host, activo]),
  );
}

function diferencia<T>(a: T[], b: T[]) {
  const setB = new Set(b);
  return a.filter((item) => !setB.has(item));
}

export function ScanComparison({ auditoriaId, escaneos }: Props) {
  const [ejecucionA, setEjecucionA] = useState("");
  const [ejecucionB, setEjecucionB] = useState("");

  const [resultadoA, setResultadoA] =
    useState<EscaneoResultResponse | null>(null);

  const [resultadoB, setResultadoB] =
    useState<EscaneoResultResponse | null>(null);

  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState("");

  const datosA = escaneos.find(
    (escaneo) => escaneo.escaneoId === ejecucionA,
  );

  const datosB = escaneos.find(
    (escaneo) => escaneo.escaneoId === ejecucionB,
  );

  const seleccionValida =
    ejecucionA !== "" &&
    ejecucionB !== "" &&
    ejecucionA !== ejecucionB;

  useEffect(() => {
    if (!seleccionValida) {
      setResultadoA(null);
      setResultadoB(null);
      setError("");
      return;
    }

    let cancelado = false;

    async function cargar() {
      try {
        setCargando(true);
        setError("");

        const [a, b] = await Promise.all([
          fetchEscaneo(auditoriaId, ejecucionA),
          fetchEscaneo(auditoriaId, ejecucionB),
        ]);

        if (cancelado) {
          return;
        }

        setResultadoA(a);
        setResultadoB(b);
      } catch (err) {
        if (cancelado) {
          return;
        }

        setResultadoA(null);
        setResultadoB(null);
        setError(
          err instanceof Error
            ? err.message
            : "No se pudieron obtener los resultados.",
        );
      } finally {
        if (!cancelado) {
          setCargando(false);
        }
      }
    }

    void cargar();

    return () => {
      cancelado = true;
    };
  }, [auditoriaId, ejecucionA, ejecucionB, seleccionValida]);

  const comparacion = useMemo(() => {
    if (!resultadoA || !resultadoB) {
      return null;
    }

    const activosA = mapaActivos(resultadoA);
    const activosB = mapaActivos(resultadoB);

    const hostsA = [...activosA.keys()];
    const hostsB = [...activosB.keys()];

    const activosNuevos = hostsB.filter(
      (host) => !activosA.has(host),
    );

    const activosEliminados = hostsA.filter(
      (host) => !activosB.has(host),
    );

    const hostsCompartidos = hostsA.filter((host) =>
      activosB.has(host),
    );

    const hosts: HostComparison[] = hostsCompartidos.map((host) => {
      const activoA = activosA.get(host);
      const activoB = activosB.get(host);

      const puertosA = mapaPuertos(activoA);
      const puertosB = mapaPuertos(activoB);

      const clavesA = [...puertosA.keys()];
      const clavesB = [...puertosB.keys()];

      const puertosNuevos = clavesB
        .filter((clave) => !puertosA.has(clave))
        .map((clave) => puertosB.get(clave)!)
        .filter(Boolean);

      const puertosEliminados = clavesA
        .filter((clave) => !puertosB.has(clave))
        .map((clave) => puertosA.get(clave)!)
        .filter(Boolean);

      const cpesA = [
        ...new Set(
          (activoA?.puertos ?? []).flatMap(
            (puerto) => puerto.cpes,
          ),
        ),
      ];

      const cpesB = [
        ...new Set(
          (activoB?.puertos ?? []).flatMap(
            (puerto) => puerto.cpes,
          ),
        ),
      ];

      return {
        host,
        hostnameA: activoA?.hostname ?? null,
        hostnameB: activoB?.hostname ?? null,
        nuevo: false,
        eliminado: false,
        puertosNuevos,
        puertosEliminados,
        cpesNuevos: diferencia(cpesB, cpesA),
        cpesEliminados: diferencia(cpesA, cpesB),
      };
    });

    return {
      activosNuevos,
      activosEliminados,
      hosts,
      puertosNuevos: hosts.reduce(
        (total, host) => total + host.puertosNuevos.length,
        0,
      ),
      puertosEliminados: hosts.reduce(
        (total, host) => total + host.puertosEliminados.length,
        0,
      ),
      cpesNuevos: hosts.reduce(
        (total, host) => total + host.cpesNuevos.length,
        0,
      ),
      cpesEliminados: hosts.reduce(
        (total, host) => total + host.cpesEliminados.length,
        0,
      ),
    };
  }, [resultadoA, resultadoB]);

  function formatearNumero(
    valor: number | null | undefined,
    decimales = 1,
  ) {
    return valor != null ? valor.toFixed(decimales) : "—";
  }

  return (
    <section
      className="dashboard-section"
      aria-labelledby="scan-comparison-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">Trazabilidad</p>

        <h2 id="scan-comparison-title">
          Comparación de ejecuciones
        </h2>

        <p>
          Compará dos ejecuciones para identificar cambios en activos,
          puertos, CPE y métricas de riesgo.
        </p>
      </div>

      <div className="comparison-selectors">
        <label>
          Ejecución A
          <select
            value={ejecucionA}
            onChange={(event) =>
              setEjecucionA(event.target.value)
            }
          >
            <option value="">Seleccionar ejecución</option>

            {escaneos.filter(scan => scan.estado === "COMPLETADO").map((escaneo) => (
              <option
                key={escaneo.escaneoId}
                value={escaneo.escaneoId}
              >
                {new Date(escaneo.fecha).toLocaleString()}
              </option>
            ))}
          </select>
        </label>

        <label>
          Ejecución B
          <select
            value={ejecucionB}
            onChange={(event) =>
              setEjecucionB(event.target.value)
            }
          >
            <option value="">Seleccionar ejecución</option>

            {escaneos.filter(scan => scan.estado === "COMPLETADO").map((escaneo) => (
              <option
                key={escaneo.escaneoId}
                value={escaneo.escaneoId}
              >
                {new Date(escaneo.fecha).toLocaleString()}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error && (
        <div className="dashboard-error">
          {error}
        </div>
      )}

      {!seleccionValida && !error && (
        <div className="dashboard-empty">
          <p>
            Seleccioná dos ejecuciones diferentes para realizar
            la comparación.
          </p>
        </div>
      )}

      {cargando && (
        <div className="dashboard-empty">
          <p>
            Cargando resultados de las ejecuciones...
          </p>
        </div>
      )}

      {seleccionValida &&
        !cargando &&
        datosA &&
        datosB &&
        comparacion && (
          <>
            <div className="comparison-metrics">
              <article className="dashboard-card">
                <span>Activos</span>
                <strong>
                  {datosA.activos} → {datosB.activos}
                </strong>
              </article>

              <article className="dashboard-card">
                <span>Puertos</span>
                <strong>
                  {datosA.puertos} → {datosB.puertos}
                </strong>
              </article>

              <article className="dashboard-card">
                <span>CVEs</span>
                <strong>
                  {datosA.cves} → {datosB.cves}
                </strong>
              </article>

              <article className="dashboard-card">
                <span>CVEs críticos</span>
                <strong>
                  {datosA.cvesCriticos} → {datosB.cvesCriticos}
                </strong>
              </article>

              <article className="dashboard-card">
                <span>CVSS promedio</span>
                <strong>
                  {formatearNumero(datosA.cvssPromedio, 2)}
                  {" → "}
                  {formatearNumero(datosB.cvssPromedio, 2)}
                </strong>
              </article>
            </div>

            <div className="comparison-metrics">
              <article className="dashboard-card">
                <span>Activos nuevos</span>
                <strong>
                  +{comparacion.activosNuevos.length}
                </strong>
              </article>

              <article className="dashboard-card">
                <span>Activos eliminados</span>
                <strong>
                  -{comparacion.activosEliminados.length}
                </strong>
              </article>

              <article className="dashboard-card">
                <span>Puertos nuevos</span>
                <strong>
                  +{comparacion.puertosNuevos}
                </strong>
              </article>

              <article className="dashboard-card">
                <span>Puertos eliminados</span>
                <strong>
                  -{comparacion.puertosEliminados}
                </strong>
              </article>

              <article className="dashboard-card">
                <span>CPE nuevos</span>
                <strong>
                  +{comparacion.cpesNuevos}
                </strong>
              </article>
            </div>

            <div className="comparison-hosts">
              <article className="dashboard-card">
                <h3>Activos nuevos</h3>

                {comparacion.activosNuevos.length === 0 ? (
                  <p>No se detectaron activos nuevos.</p>
                ) : (
                  <ul>
                    {comparacion.activosNuevos.map((host) => (
                      <li key={host}>{host}</li>
                    ))}
                  </ul>
                )}
              </article>

              <article className="dashboard-card">
                <h3>Activos eliminados</h3>

                {comparacion.activosEliminados.length === 0 ? (
                  <p>
                    No se detectaron activos eliminados.
                  </p>
                ) : (
                  <ul>
                    {comparacion.activosEliminados.map((host) => (
                      <li key={host}>{host}</li>
                    ))}
                  </ul>
                )}
              </article>
            </div>

            <div className="comparison-hosts">
              <article className="dashboard-card">
                <h3>Cambios por host</h3>

                {comparacion.hosts.length === 0 ? (
                  <p>
                    No hay activos compartidos para comparar.
                  </p>
                ) : (
                  <div className="comparison-host-list">
                    {comparacion.hosts.map((host) => (
                      <div
                        className="comparison-host-row"
                        key={host.host}
                      >
                        <div className="host-breakdown-main">
                          <strong>{host.host}</strong>

                          <span>
                            {host.hostnameB ??
                              host.hostnameA ??
                              "Sin hostname"}
                          </span>
                        </div>

                        <div className="host-breakdown-metrics">
                          <span>
                            Puertos nuevos:{" "}
                            {host.puertosNuevos.length}
                          </span>

                          <span>
                            Puertos eliminados:{" "}
                            {host.puertosEliminados.length}
                          </span>

                          <span>
                            CPE nuevos:{" "}
                            {host.cpesNuevos.length}
                          </span>

                          <span>
                            CPE eliminados:{" "}
                            {host.cpesEliminados.length}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </article>
            </div>
          </>
        )}
    </section>
  );
}
