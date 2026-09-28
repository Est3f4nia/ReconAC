import {
  apiFetch,
  parseApiResponse,
} from "./client";

import type { CveDetalle } from "./types";

export async function fetchCveDetalle(
  cveId: string,
): Promise<CveDetalle> {
  const res = await apiFetch(
    `/api/vulnerabilities/${encodeURIComponent(cveId)}`,
  );

  return parseApiResponse<CveDetalle>(
    res,
    "No se pudo obtener el detalle de la vulnerabilidad.",
  );
}