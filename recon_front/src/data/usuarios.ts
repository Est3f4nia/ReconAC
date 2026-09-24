import {
  apiFetch,
  assertApiResponseOk,
} from "./client";

import type {
  UpdateNvdApiKeyRequest,
} from "./types";

export async function updateNvdApiKey(
  apiKey: string,
): Promise<void> {
  const request: UpdateNvdApiKeyRequest = {
    apiKey,
  };

  const res = await apiFetch(
    "/api/usuarios/me/nvd-api-key",
    {
      method: "PATCH",
      body: JSON.stringify(request),
    },
  );

  await assertApiResponseOk(
    res,
    "No se pudo actualizar la API KEY.",
  );
}