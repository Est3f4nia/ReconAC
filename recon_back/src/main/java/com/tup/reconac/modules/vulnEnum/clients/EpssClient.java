package com.tup.reconac.modules.vulnEnum.clients;

import com.tup.reconac.modules.vulnEnum.dtos.data.EpssData;
import com.tup.reconac.modules.vulnEnum.dtos.EpssResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
@RequiredArgsConstructor
public class EpssClient {

    private static final String BASE_URL =
            "https://api.first.org/data/v1";

    /*
     * FIRST limita el parámetro cve a 2000 caracteres,
     * incluyendo las comas.
     */
    private static final int MAX_QUERY_LENGTH = 2000;

    private final RestClient.Builder restClientBuilder;

    public Map<String, EpssData> getEpss(
            Collection<String> cveIds
    ) {

        if (cveIds == null || cveIds.isEmpty()) {
            return Map.of();
        }

        List<String> uniqueCves =
                cveIds.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(id -> !id.isBlank())
                        .distinct()
                        .toList();

        if (uniqueCves.isEmpty()) {
            return Map.of();
        }

        Map<String, EpssData> result =
                new HashMap<>();

        List<String> batch = new ArrayList<>();
        int currentLength = 0;

        for (String cveId : uniqueCves) {

            int additionalLength =
                    batch.isEmpty()
                            ? cveId.length()
                            : cveId.length() + 1;

            if (!batch.isEmpty() &&
                    currentLength + additionalLength >
                            MAX_QUERY_LENGTH) {

                requestBatch(batch, result);

                batch.clear();
                currentLength = 0;
            }

            batch.add(cveId);

            currentLength +=
                    batch.size() == 1
                            ? cveId.length()
                            : cveId.length() + 1;
        }

        if (!batch.isEmpty()) {
            requestBatch(batch, result);
        }

        return result;
    }

    private void requestBatch(
            List<String> cveIds,
            Map<String, EpssData> result
    ) {

        String cves =
                String.join(",", cveIds);

        RestClient client =
                restClientBuilder
                        .baseUrl(BASE_URL)
                        .build();

        EpssResponse response =
                client.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/epss")
                                .queryParam("cve", cves)
                                .build()
                        )
                        .retrieve()
                        .body(EpssResponse.class);

        if (response == null ||
                response.data() == null) {

            return;
        }

        for (EpssData data : response.data()) {

            if (data.cve() != null) {

                result.put(
                        data.cve().toUpperCase(Locale.ROOT),
                        data
                );
            }
        }
    }
}