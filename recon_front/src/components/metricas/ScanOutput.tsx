import { useEffect, useRef, useState } from "react";
import { apiFetch } from "@/data/client";
import type { ScanStatusResponse } from "@/data/types";

type Line = { seq: number; timestamp: string; text: string };
type Logs = { available: boolean; lines: Line[]; nextCursor: number; truncated: boolean };

export function ScanOutput({ auditoriaId, scans, preferredId }: {
  auditoriaId: string;
  scans: ScanStatusResponse[];
  preferredId?: string;
}) {
  const [selection, setSelection] = useState("");
  const selected = scans.some(scan => scan.scanId === selection) ? selection : scans[0]?.scanId ?? "";
  const [lines, setLines] = useState<Line[]>([]);
  const [notice, setNotice] = useState("");
  const output = useRef<HTMLDivElement>(null);
  const follow = useRef(true);
  const currentStatus = useRef(scans.find(scan => scan.scanId === selected)?.status);
  currentStatus.current = scans.find(scan => scan.scanId === selected)?.status;

  useEffect(() => { if (preferredId) setSelection(preferredId); }, [preferredId]);
  useEffect(() => {
    setLines([]);
    setNotice(selected ? "Conectando con la salida del módulo…" : "Iniciá un escaneo para ver la salida de Python.");
    follow.current = true;
    if (!selected) return;
    const abort = new AbortController();
    let timer: ReturnType<typeof setTimeout>;
    let cursor = 0;
    let finishedPolls = 0;
    async function poll() {
      try {
        const response = await apiFetch(
          `/api/auditorias/${encodeURIComponent(auditoriaId)}/escaneos/${encodeURIComponent(selected)}/logs?after=${cursor}`,
          { signal: abort.signal },
        );
        if (!response.ok) throw new Error("No se pudo consultar la salida del módulo.");
        const data: Logs = await response.json();
        if (abort.signal.aborted) return;
        // El módulo puede reiniciarse y perder su búfer temporal.
        if (data.nextCursor < cursor) { cursor = 0; setLines([]); }
        else {
          cursor = data.nextCursor;
          setLines(previous => [...previous, ...data.lines.filter(line =>
            !previous.some(existing => existing.seq === line.seq))].slice(-500));
        }
        setNotice(!data.available ? "Salida no disponible: el módulo aún no inició, se reinició o el registro venció."
          : data.truncated ? "Se muestran las últimas 500 líneas disponibles."
          : "");
      } catch (error) {
        if (!abort.signal.aborted) setNotice(error instanceof Error ? error.message : "Conexión interrumpida.");
      } finally {
        if (!abort.signal.aborted) {
          const active = currentStatus.current === "PENDIENTE" || currentStatus.current === "EN_PROCESO";
          finishedPolls = active ? 0 : finishedPolls + 1;
          if (finishedPolls < 3) timer = setTimeout(poll, 2500);
        }
      }
    }
    void poll();
    return () => { abort.abort(); clearTimeout(timer); };
  }, [auditoriaId, selected]);

  useEffect(() => {
    if (follow.current && output.current) output.current.scrollTop = output.current.scrollHeight;
  }, [lines]);

  return <div className="scan-terminal-preview">
    <label className="scan-output-label" htmlFor="scan-output-selection">Salida del módulo Python</label>
    {scans.length > 0 && <select id="scan-output-selection" value={selected}
      onChange={event => setSelection(event.target.value)}>
      {scans.map(scan => <option key={scan.scanId} value={scan.scanId ?? ""}>
        {scan.scanId?.slice(0, 8)} · {scan.status.replaceAll("_", " ")}
      </option>)}
    </select>}
    <div className="scan-output-lines" ref={output} role="log" aria-label="Salida del escaneo"
      aria-live="polite" aria-relevant="additions" tabIndex={0}
      onScroll={event => {
        const element = event.currentTarget;
        follow.current = element.scrollHeight - element.scrollTop - element.clientHeight < 30;
      }}>
      {lines.map(line => <div key={line.seq} className={line.text.startsWith("[!]") ? "console-error" : ""}>
        {line.text}
      </div>)}
      {!lines.length && !notice && <span>Esperando salida del módulo…</span>}
    </div>
    {notice && <small role="status">{notice}</small>}
  </div>;
}
