import { FormEvent, useState } from "react";

import type { ScanStartRequest } from "@/data/types";

interface Props {
  onSubmit: (request: ScanStartRequest) => void | Promise<void>;
  loading?: boolean;
}

export function ScanForm({
  onSubmit,
  loading = false,
}: Props) {
  const [objetivos, setObjetivos] = useState("");
  const [nvdApiKey, setNvdApiKey] = useState("");

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();

    const objetivosNormalizados = objetivos
      .split(/\r?\n|,/)
      .map((objetivo) => objetivo.trim())
      .filter(Boolean);

    if (objetivosNormalizados.length === 0) {
      return;
    }

    const request: ScanStartRequest = {
      objetivos: objetivosNormalizados,
    };

    if (nvdApiKey.trim()) {
      request.nvdApiKey = nvdApiKey.trim();
    }

    await onSubmit(request);
  }

  return (
    <section className="dashboard-card scan-form">
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">
          Reconocimiento
        </p>

        <h2>Nuevo escaneo</h2>

        <p>
          Ingresá uno o más objetivos para iniciar una
          nueva ejecución.
        </p>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="objetivos">
            Objetivos
          </label>

          <textarea
            id="objetivos"
            name="objetivos"
            value={objetivos}
            onChange={(event) =>
              setObjetivos(event.target.value)
            }
            placeholder={
              "Ejemplo:\nscanme.nmap.org\n192.168.1.1"
            }
            rows={5}
            disabled={loading}
            required
          />

          <small>
            Podés ingresar un objetivo por línea o
            separados por comas.
          </small>
        </div>

        <div className="form-group">
          <label htmlFor="nvdApiKey">
            NVD API Key
            <span> (opcional)</span>
          </label>

          <input
            id="nvdApiKey"
            name="nvdApiKey"
            type="password"
            value={nvdApiKey}
            onChange={(event) =>
              setNvdApiKey(event.target.value)
            }
            placeholder="API key de NVD"
            disabled={loading}
            autoComplete="off"
          />
        </div>

        <button
          type="submit"
          className="button"
          disabled={
            loading || objetivos.trim().length === 0
          }
        >
          {loading
            ? "Iniciando escaneo..."
            : "Iniciar escaneo"}
        </button>
      </form>
    </section>
  );
}