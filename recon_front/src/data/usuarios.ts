import { apiFetch } from "./client";
import type {
  BaseResponse,
} from "./types";

async function parseResponse<T>(
  res: Response,
): Promise<T> {
  const body =
    await res.json().catch(
      () => null,
    );

  if (!res.ok) {
    const detail =
      body?.detail ??
      body?.message ??
      body?.errors?.[0] ??
      "No se pudo actualizar la API KEY.";

    throw new Error(
      typeof detail === "string"
        ? detail
        : "No se pudo actualizar la API KEY.",
    );
  }

  return (
    body?.data ??
    body
  ) as T;
}

export async function updateNvdApiKey(
  apiKey: string,
): Promise<void> {
  const res = await apiFetch(
    "/api/usuarios/me/nvd-api-key",
    {
      method: "PATCH",
      body: JSON.stringify({
        apiKey,
      }),
    },
  );

  await parseResponse<
    BaseResponse<null>
  >(res);
}