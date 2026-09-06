package com.tup.reconac.modules.vulnEnum.services;


import com.tup.reconac.modules.vulnEnum.dtos.data.KevData;
import com.tup.reconac.modules.vulnEnum.models.Cve;
import com.tup.reconac.modules.vulnEnum.repositories.CveRepository;
import com.tup.reconac.modules.vulnEnum.services.clients.KevClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KevService {

    private final KevClient kevClient;
    private final CveRepository cveRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Boolean> enrichKev(
            Collection<String> cveIds
    ) {

        if (cveIds == null || cveIds.isEmpty()) {
            return Map.of();
        }

        Set<String> uniqueCveIds =
                cveIds.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(id -> !id.isBlank())
                        .map(id ->
                                id.toUpperCase(Locale.ROOT)
                        )
                        .collect(Collectors.toSet());

        if (uniqueCveIds.isEmpty()) {
            return Map.of();
        }

        /*
         * Buscamos los CVE que ya existen.
         */
        List<Cve> existingCves =
                cveRepository.findAllByCveIn(
                        uniqueCveIds
                );

        Map<String, Cve> cvesPorId =
                existingCves.stream()
                        .collect(Collectors.toMap(
                                cve ->
                                        cve.getCve()
                                                .toUpperCase(
                                                        Locale.ROOT
                                                ),
                                cve -> cve
                        ));

        /*
         * CISA KEV devuelve solamente los CVE que
         * forman parte del catálogo.
         */
        Map<String, KevData> kevResults =
                kevClient.getKev(uniqueCveIds);

        Map<String, Boolean> result =
                new HashMap<>();

        for (String cveId : uniqueCveIds) {

            Cve cve =
                    cvesPorId.get(cveId);

            if (cve == null) {

                cve = new Cve();
                cve.setCve(cveId);
            }

            boolean explotacionActiva =
                    kevResults.containsKey(cveId);

            cve.setKev(
                    explotacionActiva
            );

            cveRepository.save(cve);

            result.put(
                    cveId,
                    explotacionActiva
            );
        }

        return result;
    }
}