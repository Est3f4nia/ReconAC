package com.tup.reconac.modules.vulnEnum.services;

import com.tup.reconac.modules.vulnEnum.dtos.data.EpssData;
import com.tup.reconac.modules.vulnEnum.models.Cve;
import com.tup.reconac.modules.vulnEnum.repositories.CveRepository;
import com.tup.reconac.modules.vulnEnum.clients.EpssClient;
import com.tup.reconac.modules.vulnEnum.services.helpers.VulnerabilityCatalogLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EpssService {

    private final EpssClient epssClient;
    private final CveRepository cveRepository;
    private final VulnerabilityCatalogLock catalogLock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, BigDecimal> enrichEpss(Collection<String> cveIds) {

        if (cveIds == null || cveIds.isEmpty()) return Map.of();

        Set<String> uniqueCveIds = cveIds
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .map(id -> id.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        if (uniqueCveIds.isEmpty()) return Map.of();

        catalogLock.acquire();

        List<Cve> existingCves = cveRepository.findAllByCveIn(uniqueCveIds);

        Map<String, Cve> cvesPorId = existingCves
                .stream()
                .collect(Collectors.toMap(cve ->
                                cve.getCve().toUpperCase(Locale.ROOT),
                        cve -> cve)
                );

        Map<String, BigDecimal> result = new HashMap<>();
        List<String> missingEpss = new ArrayList<>();

        /*
         * reutilización EPSS persistido
         */

        for (String cveId : uniqueCveIds) {
            Cve cve = cvesPorId.get(cveId);

            if (cve != null && cve.getEpss() != null) {
                result.put(cveId, cve.getEpss());
            } else {
                missingEpss.add(cveId);
            }
        }


        if (!missingEpss.isEmpty()) {

            Map<String, EpssData> externalResults = epssClient.getEpss(missingEpss);

            for (String cveId : missingEpss) {
                EpssData data = externalResults.get(cveId);

                if (data == null || data.epss() == null) continue;

                BigDecimal epss = data.epss();
                Cve cve = cvesPorId.get(cveId);

                if (cve == null) {
                    cve = new Cve();
                    cve.setCve(cveId);
                }

                cve.setEpss(epss);

                Cve saved = cveRepository.save(cve);
                result.put(cveId, saved.getEpss());
            }
        }

        return result;
    }
}
