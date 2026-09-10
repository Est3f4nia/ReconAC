package com.tup.reconac.modules.vulnEnum.services;

import com.tup.reconac.modules.vulnEnum.dtos.data.KevData;
import com.tup.reconac.modules.vulnEnum.models.Cve;
import com.tup.reconac.modules.vulnEnum.repositories.CveRepository;
import com.tup.reconac.modules.vulnEnum.clients.KevClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KevService {

    // catálogo KEV cacheado en memoria DE SPRING
    // uso exclusivo del back y es un catálogo chico

    private static final Duration CATALOG_TTL = Duration.ofHours(6);
    private final KevClient kevClient;
    private final CveRepository cveRepository;
    private final VulnerabilityCatalogLock catalogLock;

    // volatile: los threads ven la última referencia publicada después de una actualización
    private volatile Map<String, KevData> cachedCatalog = Map.of();
    private volatile Instant catalogExpiresAt = Instant.EPOCH;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Boolean> enrichKev(Collection<String> cveIds) {

        if (cveIds == null || cveIds.isEmpty()) return Map.of();

        Set<String> uniqueCveIds = cveIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .map(id -> id.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        if (uniqueCveIds.isEmpty()) return Map.of();

        catalogLock.acquire();

        List<Cve> existingCves = cveRepository.findAllByCveIn(uniqueCveIds);

        Map<String, Cve> cvesPorId = existingCves.stream()
                .collect(Collectors.toMap(
                        cve -> cve.getCve().toUpperCase(Locale.ROOT),
                        cve -> cve
                ));

        Map<String, KevData> kevCatalog = getCachedCatalog();
        Map<String, Boolean> result = new HashMap<>();

        for (String cveId : uniqueCveIds) {
            Cve cve = cvesPorId.get(cveId);
            if (cve == null) continue;

            boolean explotacionConocida = kevCatalog.containsKey(cveId);
            cve.setKev(explotacionConocida);

            result.put(cveId, explotacionConocida);
        }

        // evita ejecutar un save() individual por cada CVE
        cveRepository.saveAll(existingCves);
        return result;
    }

    // devuelve el catálogo KEV cacheado y descarga una nueva versión si no está o si expiró
    private Map<String, KevData> getCachedCatalog() {

        Instant now = Instant.now();
        if (!cachedCatalog.isEmpty() && now.isBefore(catalogExpiresAt)) return cachedCatalog;

        synchronized (this) {

            // Double-check: otro thread pudo actualizar el catálogo
            now = Instant.now();
            if (!cachedCatalog.isEmpty() && now.isBefore(catalogExpiresAt)) return cachedCatalog;

            Map<String, KevData> downloaded = kevClient.getCatalog();

            if (downloaded != null && !downloaded.isEmpty()) {
                cachedCatalog = Map.copyOf(downloaded);
                catalogExpiresAt = now.plus(CATALOG_TTL);
            }

            return cachedCatalog;
        }
    }
}