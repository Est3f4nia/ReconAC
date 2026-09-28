package com.tup.reconac.modules.vulnEnum.clients;

import com.tup.reconac.modules.vulnEnum.dtos.enrichment.KevResponse;
import com.tup.reconac.modules.vulnEnum.dtos.data.KevData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KevClient {

    private static final String BASE_URL = "https://www.cisa.gov/sites/default/files/feeds";
    private static final String CATALOG_PATH = "/known_exploited_vulnerabilities.json";

    private final RestClient.Builder restClientBuilder;

    public Map<String, KevData> getCatalog() {

        RestClient client = restClientBuilder
                .baseUrl(BASE_URL)
                .build();

        KevResponse response = client.get()
                .uri(CATALOG_PATH)
                .retrieve()
                .body(KevResponse.class);

        if (response == null || response.vulnerabilities() == null) return Map.of();

        Map<String, KevData> result = new HashMap<>();

        for (KevData data : response.vulnerabilities()) {

            if (data == null || data.cveID() == null || data.cveID().isBlank()) continue;

            String cveId = data.cveID()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            result.put(cveId, data);
        }

        return result;
    }
}
