import { apiFetch } from "./client";
import type {
    ActivoAgrupadoResponse,
    ActivoRequest,
    ActivoResponse,
    PageResponse,
} from "@/data/types";

async function parseResponse<T>(res: Response): Promise<T> {
    const body = await res.json();

    if (!res.ok) {
        const detail =
            body.detail ??
            body.message ??
            body.errors ??
            "Error desconocido";

        throw new Error(
            typeof detail === "string"
                ? detail
                : "Error en la petición."
        );
    }

    return body.data ?? body;
}

export async function fetchActivos(
    page = 0,
    size = 20
): Promise<PageResponse<ActivoAgrupadoResponse>> {
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort: "host,asc",
    });

    const res = await apiFetch(
        `/api/activos?${params.toString()}`
    );

    return parseResponse<PageResponse<ActivoAgrupadoResponse>>(res);
}

export async function fetchActivosByEscaneo(
    escaneoId: string,
    page = 0,
    size = 20
): Promise<PageResponse<ActivoResponse>> {
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort: "host,asc",
    });

    const res = await apiFetch(
        `/api/activos/by-escaneo/${encodeURIComponent(escaneoId)}?${params.toString()}`
    );

    return parseResponse<PageResponse<ActivoResponse>>(res);
}

export async function createActivo(
    data: ActivoRequest
): Promise<ActivoResponse> {
    const res = await apiFetch("/api/activos", {
        method: "POST",
        body: JSON.stringify(data),
    });

    return parseResponse<ActivoResponse>(res);
}

export async function updateActivo(
    id: string,
    data: ActivoRequest
): Promise<ActivoResponse> {
    const res = await apiFetch(
        `/api/activos/${encodeURIComponent(id)}`,
        {
            method: "PATCH",
            body: JSON.stringify(data),
        }
    );

    return parseResponse<ActivoResponse>(res);
}

export async function deleteActivo(id: string): Promise<void> {
    const res = await apiFetch(
        `/api/activos/${encodeURIComponent(id)}`,
        {
            method: "DELETE",
        }
    );

    if (!res.ok) {
        const body = await res.json().catch(() => null);

        const detail =
            body?.detail ??
            body?.message ??
            "No se pudo eliminar el activo.";

        throw new Error(detail);
    }
}