package com.tup.reconac.cache;

import com.tup.reconac.modules.vulnEnum.dtos.nvd.NvdCacheEntry;
import com.tup.reconac.modules.vulnEnum.dtos.nvd.NvdReferenceData;
import com.tup.reconac.modules.vulnEnum.dtos.nvd.NvdVulnerabilityData;
import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.models.CpeCve;
import com.tup.reconac.modules.vulnEnum.models.Cve;
import com.tup.reconac.modules.vulnEnum.models.CveCwe;
import com.tup.reconac.modules.vulnEnum.models.Cwe;
import com.tup.reconac.modules.vulnEnum.models.Referencia;
import com.tup.reconac.modules.vulnEnum.repositories.CpeCveRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CpeRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CveCweRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CweRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CveRepository;
import com.tup.reconac.modules.vulnEnum.repositories.ReferenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CacheService {

    private static final String PREFIX_NVD = "cache:nvd:v2:cpe:";
    private final RedisTemplate<String, Object> redis;
    private final CpeRepository cpeRepository;
    private final CveRepository cveRepository;
    private final CweRepository cweRepository;
    private final CpeCveRepository cpeCveRepository;
    private final CveCweRepository cveCweRepository;
    private final ReferenciaRepository referenciaRepository;

    @Value("${cache.ttl-hours:24}")
    private long ttlHours;

    // Read Through
    public Optional<NvdCacheEntry> getNvd(String cpeUri) {
        System.out.println("[CacheService] getNvd(" + cpeUri + ")");
        NvdCacheEntry cached = getFromRedis(cpeUri);

        if (cached != null) {
            System.out.println("[CacheService] Se obtuvo: " + cached);
            return Optional.of(cached);
        }

        return loadFromDatabase(cpeUri);
    }

    public void putNvd(NvdCacheEntry entry) {
        System.out.println("[CacheService] putNvd(" + entry.cpe() + ")");
        redis.opsForValue().set(
                PREFIX_NVD + entry.cpe(),
                entry,
                Duration.ofHours(ttlHours)
        );
        System.out.println("[CacheService] Almacenamiento exitoso de: " + entry.cpe());
    }

    public boolean isFresh(NvdCacheEntry entry) {

        if (entry == null || entry.lastChecked() == null) return false;
        return entry.lastChecked()
                .plusHours(ttlHours)
                .isAfter(LocalDateTime.now());
    }

    public void invalidateNvd(String cpeUri) {
        redis.delete(PREFIX_NVD + cpeUri);
    }

    private NvdCacheEntry getFromRedis(String cpeUri) {
        System.out.println("[CacheService] getFromRedis(" + cpeUri + ")");

        Object value = redis.opsForValue().get(PREFIX_NVD + cpeUri);
        if (value instanceof NvdCacheEntry entry) {
            System.out.println("[CacheService] Se obtuvo: " + entry.cpe());
            return entry;
        }
        return null;
    }

    private Optional<NvdCacheEntry> loadFromDatabase(String cpeUri) {
        System.out.println("[CacheService] loadFromDatabase(" + cpeUri + ")");

        Optional<Cpe> cpeOptional = cpeRepository.findByUri(cpeUri);
        if (cpeOptional.isEmpty()) return Optional.empty();

        Cpe cpe = cpeOptional.get();
        if (cpe.getUltimoCheck() == null) return Optional.empty();
        List<CpeCve> relations = cpeCveRepository.findByCpeId(cpe.getId());
        List<NvdVulnerabilityData> vulnerabilities = new ArrayList<>();

        for (CpeCve relation : relations) {

            Optional<Cve> cveOptional = cveRepository.findById(relation.getCveId());
            if (cveOptional.isEmpty()) continue;

            Cve cve = cveOptional.get();
            List<String> cwes =cveCweRepository
                    .findByCveId(cve.getId())
                    .stream()
                    .map(CveCwe::getCweId)
                    .map(cweRepository::findById)
                    .flatMap(Optional::stream)
                    .map(Cwe::getCweCode)
                    .toList();

            List<NvdReferenceData> references = referenciaRepository
                    .findByCveId(cve.getId())
                    .stream()
                    .map(this::toReferenceData)
                    .toList();

            List<String> exploitRefs = splitExploitRefs(cve.getExploitRefs());

            vulnerabilities.add(new NvdVulnerabilityData(
                    cve.getCve(),
                    cve.getDescripcion(),
                    cve.getSeveridad(),
                    cve.getCvss(),
                    cve.getVectorCvss(),
                    cve.getFechaPublicacion(),
                    cve.getUltModificacion(),
                    cwes,
                    references,
                    exploitRefs,
                    cve.getUrlNist(),
                    cve.getMitigacion(),
                    cve.getVersionVuln(),
                    cve.getTipoParche(),
                    cve.getVersionParche()
                    )
            );
        }

        NvdCacheEntry entry = new NvdCacheEntry(cpeUri, cpe.getUltimoCheck(), vulnerabilities);
        putNvd(entry);
        System.out.println("[CacheService] Se obtuvo: " + entry.cpe());
        return Optional.of(entry);
    }

    private NvdReferenceData toReferenceData(Referencia referencia) {

        List<String> tags = referencia.getTags() == null ? List.of() : List.of(referencia.getTags());

        return new NvdReferenceData(
                referencia.getUrl(),
                referencia.getSource(),
                tags
        );
    }

    private List<String> splitExploitRefs(String value) {

        if (value == null || value.isBlank()) return List.of();
        return List.of( value.split("\\R"));
    }
}
