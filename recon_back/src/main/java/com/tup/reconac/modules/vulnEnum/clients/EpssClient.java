package com.tup.reconac.modules.vulnEnum.clients;

import com.tup.reconac.modules.vulnEnum.dtos.data.EpssData;
import com.tup.reconac.modules.vulnEnum.dtos.enrichment.EpssResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
@RequiredArgsConstructor
public class EpssClient {

    private static final String BASE_URL = "https://api.first.org/data/v1";
    private static final int MAX_QUERY_LENGTH = 2000;

    private final RestClient.Builder restClientBuilder;

    public Map<String, EpssData> getEpss(Collection<String> cveIds) {

        List<String> uniqueCves = normalizeCves(cveIds);
        if (uniqueCves.isEmpty()) return Map.of();

        RestClient client = restClientBuilder
                .baseUrl(BASE_URL)
                .build();

        Map<String, EpssData> result = new HashMap<>();
        List<String> batch = new ArrayList<>();
        int currentLength = 0;

        for (String cveId : uniqueCves) {

            if (cveId.length() > MAX_QUERY_LENGTH) {
                throw new IllegalArgumentException(
                        "Identificador CVE demasiado largo: " + cveId
                );
            }

            int additionalLength = batch.isEmpty()
                    ? cveId.length()
                    : cveId.length() + 1;

            if (!batch.isEmpty() && currentLength + additionalLength > MAX_QUERY_LENGTH) {
                requestBatch(client, batch, result);
                batch.clear();
                currentLength = 0;
            }

            batch.add(cveId);
            currentLength += batch.size() == 1
                    ? cveId.length()
                    : cveId.length() + 1;
        }

        if (!batch.isEmpty()) requestBatch(client, batch, result);

        return result;
    }

    private List<String> normalizeCves(Collection<String> cveIds) {

        if (cveIds == null || cveIds.isEmpty()) return List.of();

        return cveIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .map(id -> id.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private void requestBatch(RestClient client, List<String> cveIds, Map<String, EpssData> result) {

        String cves = String.join(",", cveIds);

        EpssResponse response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/epss")
                        .queryParam("cve", cves)
                        .build()
                )
                .retrieve()
                .body(EpssResponse.class);

        if (response == null || response.data() == null) return;

        for (EpssData data : response.data()) {

            if (data == null || data.cve() == null || data.cve().isBlank()) continue;

            result.put(
                    data.cve().toUpperCase(Locale.ROOT),
                    data
            );
        }
    }
}