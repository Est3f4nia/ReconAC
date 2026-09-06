package com.tup.reconac.modules.vulnEnum.services.clients;

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

    private static final String BASE_URL =
            "https://www.cisa.gov/sites/default/files/feeds";

    private final RestClient.Builder restClientBuilder;

    /**
     * Descarga el catálogo completo de CISA KEV y lo indexa por CVE.
     */
    public Map<String, KevData> getCatalog() {

        RestClient client = restClientBuilder
                .baseUrl(BASE_URL)
                .build();

        KevResponse response = client.get()
                .uri("/known_exploited_vulnerabilities.json")
                .retrieve()
                .body(KevResponse.class);

        if (response == null || response.vulnerabilities() == null) {
            return Map.of();
        }

        Map<String, KevData> result = new HashMap<>();

        for (KevData data : response.vulnerabilities()) {

            if (data == null || data.cveID() == null) {
                continue;
            }

            result.put(
                    data.cveID().toUpperCase(Locale.ROOT),
                    data
            );
        }

        return result;
    }

    /**
     * Devuelve únicamente los CVE solicitados que están presentes
     * en el catálogo KEV.
     */
    public Map<String, KevData> getKev(Collection<String> cveIds) {

        if (cveIds == null || cveIds.isEmpty()) {
            return Map.of();
        }

        Map<String, KevData> catalog = getCatalog();

        Map<String, KevData> result = new HashMap<>();

        for (String cveId : cveIds) {

            if (cveId == null || cveId.isBlank()) {
                continue;
            }

            KevData data = catalog.get(
                    cveId.trim().toUpperCase(Locale.ROOT)
            );

            if (data != null) {
                result.put(
                        cveId.trim().toUpperCase(Locale.ROOT),
                        data
                );
            }
        }

        return result;
    }
}
