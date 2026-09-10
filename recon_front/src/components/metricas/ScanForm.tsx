import { FormEvent, useState } from "react";
import type { ScanStartRequest } from "@/data/types";
import "./styles/ScanForm.css";
import { ScanOutput } from "./ScanOutput";
import type { ScanStatusResponse } from "@/data/types";

interface Props {
  onSubmit: (request: ScanStartRequest) => void | Promise<void>;
  loading?: boolean;
  auditoriaId: string;
  scans: ScanStatusResponse[];
  preferredScanId?: string;
}

export function ScanForm({ onSubmit, loading = false, auditoriaId, scans, preferredScanId }: Props) {
  const [objetivos, setObjetivos] = useState("");
  const [timeout, setTimeoutValue] = useState("600");
  const [icmpTimeout, setIcmpTimeout] = useState("5");
  const [maxCveYears, setMaxCveYears] = useState("2");
  const [minCvssScore, setMinCvssScore] = useState("0");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const objetivosNormalizados = objetivos
      .split(/\r?\n|,/)
      .map((objetivo) => objetivo.trim())
      .filter(Boolean);

    if (objetivosNormalizados.length === 0) {
      return;
    }

    const timeoutValue = Number(timeout);
    const icmpTimeoutValue = Number(icmpTimeout);
    const maxCveYearsValue = Number(maxCveYears);
    const minCvssScoreValue = Number(minCvssScore);

    const request: ScanStartRequest = {
      objetivos: objetivosNormalizados,
      timeout:
        Number.isFinite(timeoutValue) && timeoutValue > 0
          ? timeoutValue
          : 600,
      icmpTimeout:
        Number.isFinite(icmpTimeoutValue) && icmpTimeoutValue > 0
          ? icmpTimeoutValue
          : 5,
      maxCveYears:
        Number.isFinite(maxCveYearsValue) && maxCveYearsValue >= 0
          ? maxCveYearsValue
          : 2,
      minCvssScore:
        Number.isFinite(minCvssScoreValue) &&
        minCvssScoreValue >= 0 &&
        minCvssScoreValue <= 10
          ? minCvssScoreValue
          : 0,
    };

    await onSubmit(request);
  }

  return (
    <section
      className="dashboard-section scan-panel"
      aria-labelledby="scan-form-title"
    >
      <div className="scan-start">
        <div className="scan-panel-heading">
          <div>
            <p className="dashboard-section-eyebrow">Reconocimiento</p>
            <h2 id="scan-form-title">Nuevo escaneo</h2>
            <p>
              Ingresá uno o más objetivos para iniciar una nueva ejecución.
            </p>
          </div>
        </div>

        <form className="scan-form" onSubmit={handleSubmit}>
          {/* Columna 1: Objetivos */}
          <div className="scan-form-main">
            <div className="form-group">
              <label htmlFor="objetivos">Objetivos</label>
              <textarea
                id="objetivos"
                name="objetivos"
                value={objetivos}
                onChange={(event) => setObjetivos(event.target.value)}
                placeholder="scanme.nmap.org, 192.168.1.1"
                rows={4}
                disabled={loading}
                required
              />
              <small>
                Uno o más hosts, IPs o dominios separados por comas.
              </small>
            </div>
          </div>

          {/* Columna 2: Filtros verticales */}
          <div className="scan-filters">
            <div className="scan-filters-title">Filtros</div>

            <div className="form-group">
              <label htmlFor="timeout">Timeout Nmap</label>
              <div className="scan-input-unit">
                <input
                  id="timeout"
                  name="timeout"
                  type="number"
                  min="1"
                  step="1"
                  value={timeout}
                  onChange={(event) => setTimeoutValue(event.target.value)}
                  disabled={loading}
                />
                <span>seg.</span>
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="icmpTimeout">Timeout ICMP</label>
              <div className="scan-input-unit">
                <input
                  id="icmpTimeout"
                  name="icmpTimeout"
                  type="number"
                  min="1"
                  step="1"
                  value={icmpTimeout}
                  onChange={(event) => setIcmpTimeout(event.target.value)}
                  disabled={loading}
                />
                <span>seg.</span>
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="maxCveYears">Antigüedad máxima de CVE</label>
              <div className="scan-input-unit">
                <input
                  id="maxCveYears"
                  name="maxCveYears"
                  type="number"
                  min="0"
                  step="1"
                  value={maxCveYears}
                  onChange={(event) => setMaxCveYears(event.target.value)}
                  disabled={loading}
                />
                <span>años</span>
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="minCvssScore">Puntuación CVSS mínima</label>
              <div className="scan-input-unit">
                <input
                  id="minCvssScore"
                  name="minCvssScore"
                  type="number"
                  min="0"
                  max="10"
                  step="0.1"
                  value={minCvssScore}
                  onChange={(event) => setMinCvssScore(event.target.value)}
                  disabled={loading}
                />
                <span>/ 10</span>
              </div>
            </div>
          </div>

          {/* Columna 3: Consola + botón debajo */}
          <div className="console scan-form-aside">
            <ScanOutput auditoriaId={auditoriaId} scans={scans} preferredId={preferredScanId} />

            <button
              type="submit"
              className={`scan-submit ${loading ? "is-loading" : ""}`}
              disabled={loading || objetivos.trim().length === 0}
              aria-label={loading ? "Iniciando escaneo" : "Iniciar escaneo"}
              title={loading ? "Iniciando escaneo..." : "Iniciar escaneo"}
            >
              <span className="scan-submit-ring" />
              <span className="scan-submit-core">
                {loading ? (
                  <span className="scan-submit-dots">···</span>
                ) : (
                  <span className="scan-submit-icon">&gt;</span>
                )}
              </span>
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}
