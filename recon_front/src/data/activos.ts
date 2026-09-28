import {
  apiFetch,
  assertApiResponseOk,
  parseApiResponse,
} from "./client";

import type {
  PageActivoAgrupadoResponse,
  PageActivoResponse,
} from "./types";

export async function fetchActivos(
  page = 0,
  size = 20,
): Promise<PageActivoAgrupadoResponse> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "host,asc",
  });

  const res = await apiFetch(
    `/api/activos?${params.toString()}`,
  );

  return parseApiResponse<PageActivoAgrupadoResponse>(res);
}

export async function fetchActivosByEscaneo(
  escaneoId: string,
  page = 0,
  size = 20,
): Promise<PageActivoResponse> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "host,asc",
  });

  const res = await apiFetch(
    `/api/activos/by-escaneo/${encodeURIComponent(escaneoId)}?${params.toString()}`,
  );

  return parseApiResponse<PageActivoResponse>(res);
}

export async function deleteActivo(
  id: string,
): Promise<void> {
  const res = await apiFetch(
    `/api/activos/${encodeURIComponent(id)}`,
    {
      method: "DELETE",
    },
  );

  await assertApiResponseOk(
    res,
    "No se pudo eliminar el activo.",
  );
}