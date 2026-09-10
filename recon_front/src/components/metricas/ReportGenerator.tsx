import { useState } from "react";
import { descargarReporte } from "@/data/escaneos";
import type { EjecucionHistorial } from "@/data/types";

interface Props {
  auditoriaId: string;
  escaneos: EjecucionHistorial[];
}

export function ReportGenerator({ auditoriaId, escaneos }: Props) {
  const [escaneoId, setEscaneoId] = useState("");
  const [generando, setGenerando] = useState<"MD" | "CSV" | null>(null);
  const [error, setError] = useState("");

  const escaneosCompletados = escaneos.filter(
    (escaneo) => escaneo.estado === "COMPLETADO",
  );

  async function generarReporte(formato: "MD" | "CSV") {
    if (!escaneoId) {
      setError("Seleccioná una ejecución para generar el informe.");
      return;
    }

    try {
      setGenerando(formato);
      setError("");

      const blob = await descargarReporte(
        auditoriaId,
        escaneoId,
        formato,
      );

      const url = URL.createObjectURL(blob);
      const enlace = document.createElement("a");

      enlace.href = url;
      enlace.download = `reconac-${escaneoId}.${
        formato === "MD" ? "md" : "zip"
      }`;

      document.body.appendChild(enlace);
      enlace.click();
      enlace.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudo generar el informe.",
      );
    } finally {
      setGenerando(null);
    }
  }

  return (
    <section
      className="dashboard-section"
      aria-labelledby="report-generator-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">Exportación</p>
        <h2 id="report-generator-title">Generar informe</h2>
        <p>
          Exportá los resultados de una ejecución de esta auditoría para su
          análisis posterior.
        </p>
      </div>

      {escaneosCompletados.length === 0 ? (
        <div className="dashboard-empty">
          <p>
            No hay ejecuciones completadas disponibles para generar informes.
          </p>
        </div>
      ) : (
        <>
          <div className="report-form">
            <label htmlFor="report-scan">Ejecución</label>

            <select
              id="report-scan"
              value={escaneoId}
              onChange={(event) => {
                setEscaneoId(event.target.value);
                setError("");
              }}
              disabled={generando !== null}
            >
              <option value="">Seleccioná una ejecución</option>

              {escaneosCompletados.map((escaneo) => (
                <option
                  key={escaneo.escaneoId}
                  value={escaneo.escaneoId}
                >
                  {new Date(escaneo.fecha).toLocaleString()}{" "}
                  — {escaneo.escaneoId.slice(0, 8)}
                </option>
              ))}
            </select>
          </div>

          {error && (
            <div className="dashboard-error" role="alert">
              {error}
            </div>
          )}

          <div className="report-actions">
            <button
              className="button"
              type="button"
              onClick={() => generarReporte("MD")}
              disabled={!escaneoId || generando !== null}
            >
              {generando === "MD"
                ? "Generando..."
                : "Exportar Markdown"}
            </button>

            <button
              className="button"
              type="button"
              onClick={() => generarReporte("CSV")}
              disabled={!escaneoId || generando !== null}
            >
              {generando === "CSV"
                ? "Generando..."
                : "Exportar CSV"}
            </button>
          </div>
        </>
      )}
    </section>
  );
}