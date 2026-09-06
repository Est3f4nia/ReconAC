import { useState } from "react";
import type {
  EjecucionHistorial,
} from "@/data/types";

interface Props {
  escaneos: EjecucionHistorial[];
}

export function ScanComparison({
  escaneos,
}: Props) {
  const [ejecucionA, setEjecucionA] =
    useState("");

  const [ejecucionB, setEjecucionB] =
    useState("");

  const seleccionValida =
    ejecucionA &&
    ejecucionB &&
    ejecucionA !== ejecucionB;

  return (
    <section
      className="dashboard-section"
      aria-labelledby="scan-comparison-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">
          Trazabilidad
        </p>

        <h2 id="scan-comparison-title">
          Comparación de ejecuciones
        </h2>

        <p>
          Compará distintas ejecuciones sobre un mismo
          activo para identificar cambios.
        </p>
      </div>

      <div className="comparison-selectors">
        <label>
          Ejecución A

          <select
            value={ejecucionA}
            onChange={(event) =>
              setEjecucionA(
                event.target.value,
              )
            }
          >
            <option value="">
              Seleccionar ejecución
            </option>

            {escaneos.map((escaneo) => (
              <option
                key={escaneo.escaneoId}
                value={escaneo.escaneoId}
              >
                {new Date(
                  escaneo.fecha,
                ).toLocaleString()}
              </option>
            ))}
          </select>
        </label>

        <label>
          Ejecución B

          <select
            value={ejecucionB}
            onChange={(event) =>
              setEjecucionB(
                event.target.value,
              )
            }
          >
            <option value="">
              Seleccionar ejecución
            </option>

            {escaneos.map((escaneo) => (
              <option
                key={escaneo.escaneoId}
                value={escaneo.escaneoId}
              >
                {new Date(
                  escaneo.fecha,
                ).toLocaleString()}
              </option>
            ))}
          </select>
        </label>
      </div>

      {!seleccionValida && (
        <div className="dashboard-empty">
          <p>
            Seleccioná dos ejecuciones diferentes
            para realizar la comparación.
          </p>
        </div>
      )}

      {seleccionValida && (
        <div className="comparison-placeholder">
          <p>
            La comparación detallada estará disponible
            cuando el backend exponga las métricas
            correspondientes.
          </p>
        </div>
      )}
    </section>
  );
}