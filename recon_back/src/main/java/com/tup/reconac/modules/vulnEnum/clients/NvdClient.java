package com.tup.reconac.modules.vulnEnum.clients;

import com.tup.reconac.exceptions.vulnEnum.NvdLookupException;
import com.tup.reconac.exceptions.vulnEnum.NvdTooManyResultsException;
import com.tup.reconac.modules.vulnEnum.clients.nvd.NvdCpeQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
public class NvdClient {

    private static final String BASE_URL =
            "https://services.nvd.nist.gov/rest/json/cves/2.0/";

    private static final int RESULTS_PER_PAGE = 2000;
    private static final int MAX_RESULTS = 20_000;
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 6000;

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public List<JsonNode> findByCpe(
            String cpe,
            String apiKey
    ) {

        if (cpe == null || cpe.isBlank()) {
            return List.of();
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "NVD API key no configurada"
            );
        }

        String normalizedCpe = cpe.trim();

        String formatted =
                NvdCpeQuery.formatted(normalizedCpe);

        String parameter =
                NvdCpeQuery.parameter(normalizedCpe);

        List<JsonNode> vulnerabilities =
                new ArrayList<>();

        int startIndex = 0;

        while (true) {

            URI uri = buildUri(
                    parameter,
                    formatted,
                    startIndex
            );

            JsonNode response =
                    requestWithRetry(uri, apiKey);

            validateResponse(response);

            JsonNode vulnerabilitiesNode =
                    response.path("vulnerabilities");

            int totalResults =
                    response.path("totalResults")
                            .asInt(0);

            /*
             * Protección secundaria.
             *
             * Normalmente las CPE demasiado genéricas ya fueron
             * descartadas por NvdCpePolicy en VulnerabilityService.
             *
             * Esto protege ante consultas que, aun siendo válidas,
             * produzcan un volumen inesperadamente grande.
             */
            if (totalResults > MAX_RESULTS) {
                throw new NvdTooManyResultsException(
                        "La consulta a " + cpe + "trajo demasiados resultados"
                );
            }

            if (vulnerabilitiesNode.isEmpty()) {

                if (totalResults > startIndex) {
                    throw new NvdLookupException(
                            "NVD devolvió una página vacía antes "
                                    + "de completar los resultados para "
                                    + formatted
                    );
                }

                break;
            }

            vulnerabilities.addAll(
                    StreamSupport.stream(
                                    vulnerabilitiesNode.spliterator(),
                                    false
                            )
                            .map(node -> node.path("cve"))
                            .filter(node ->
                                    node != null
                                            && !node.isMissingNode()
                                            && !node.isNull()
                            )
                            .toList()
            );

            startIndex += vulnerabilitiesNode.size();

            if (startIndex >= totalResults) {
                break;
            }
        }

        return vulnerabilities;
    }

    private URI buildUri(
            String parameter,
            String formatted,
            int startIndex
    ) {

        String encodedCpe =
                UriUtils.encode(
                        formatted,
                        StandardCharsets.UTF_8
                );

        return URI.create(
                BASE_URL
                        + "?"
                        + parameter
                        + "="
                        + encodedCpe
                        + "&resultsPerPage="
                        + RESULTS_PER_PAGE
                        + "&startIndex="
                        + startIndex
        );
    }

    private void validateResponse(
            JsonNode response
    ) {

        if (response == null) {
            throw new NvdLookupException(
                    "NVD no devolvió una respuesta válida"
            );
        }

        JsonNode vulnerabilities =
                response.path("vulnerabilities");

        if (!vulnerabilities.isArray()
                || !response.has("totalResults")) {

            throw new NvdLookupException(
                    "Respuesta NVD incompleta"
            );
        }
    }

    private JsonNode requestWithRetry(
            URI uri,
            String apiKey
    ) {

        RestClient client =
                restClientBuilder.build();

        for (
                int attempt = 0;
                attempt < MAX_RETRIES;
                attempt++
        ) {

            try {

                String body =
                        client.get()
                                .uri(uri)
                                .headers(headers ->
                                        headers.set(
                                                "apiKey",
                                                apiKey
                                        )
                                )
                                .retrieve()
                                .body(String.class);

                if (body == null || body.isBlank()) {
                    throw new NvdLookupException(
                            "NVD devolvió una respuesta vacía"
                    );
                }

                return objectMapper.readTree(body);

            } catch (RestClientResponseException e) {

                int status =
                        e.getStatusCode().value();

                boolean retryable =
                        status == 429
                                || status >= 500;

                if (!retryable
                        || attempt == MAX_RETRIES - 1) {

                    throw buildHttpException(
                            e,
                            status,
                            apiKey
                    );
                }

                long delay =
                        status == 429
                                ? RETRY_DELAY_MS
                                  * (1L << attempt)
                                : RETRY_DELAY_MS;

                sleep(delay);

            } catch (NvdLookupException e) {
                throw e;

            } catch (Exception e) {

                if (attempt == MAX_RETRIES - 1) {
                    throw new NvdLookupException(
                            "No se pudo consultar NVD ("
                                    + e.getClass()
                                    .getSimpleName()
                                    + ")",
                            e
                    );
                }

                sleep(RETRY_DELAY_MS);
            }
        }

        throw new NvdLookupException(
                "Se agotaron los reintentos NVD"
        );
    }

    private NvdLookupException buildHttpException(
            RestClientResponseException e,
            int status,
            String apiKey
    ) {

        String detail =
                e.getResponseHeaders() == null
                        ? null
                        : e.getResponseHeaders()
                        .getFirst("message");

        if (detail != null) {

            detail = detail
                    .replace(
                            apiKey,
                            "[redacted]"
                    )
                    .replaceAll(
                            "[\\r\\n]",
                            " "
                    );

            detail = detail.substring(
                    0,
                    Math.min(
                            detail.length(),
                            300
                    )
            );
        }

        return new NvdLookupException(
                "NVD HTTP "
                        + status
                        + (
                        detail == null
                                || detail.isBlank()
                                ? ""
                                : ": " + detail
                )
        );
    }

    private void sleep(
            long milliseconds
    ) {

        try {

            Thread.sleep(milliseconds);

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            throw new NvdLookupException(
                    "La espera de reintento NVD fue interrumpida",
                    e
            );
        }
    }
}