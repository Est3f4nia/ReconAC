package com.tup.reconac.modules.vulnEnum.clients;

import com.tup.reconac.modules.vulnEnum.dtos.KevResponse;
import com.tup.reconac.modules.vulnEnum.dtos.data.KevData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KevClient {

    private static final String BASE_URL = "https://www.cisa.gov/sites/default/files/feeds";
    private final RestClient.Builder restClientBuilder;

    public Map<String, KevData> getCatalog() {
        RestClient client = restClientBuilder.baseUrl(BASE_URL).build();

        KevResponse response = client.get()
                .uri("/known_exploited_vulnerabilities.json")
                .retrieve()
                .body(KevResponse.class);

        if (response == null || response.vulnerabilities() == null) return Map.of();

        Map<String, KevData> result = new HashMap<>();
        for (KevData data : response.vulnerabilities()) {

            if (data == null || data.cveID() == null) continue;

            result.put(
                    data.cveID().toUpperCase(Locale.ROOT),
                    data
            );
        }

        return result;
    }

}
