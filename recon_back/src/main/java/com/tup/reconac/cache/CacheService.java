package com.tup.reconac.cache;

import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.models.CpeCve;
import com.tup.reconac.modules.vulnEnum.models.Cve;
import com.tup.reconac.modules.vulnEnum.models.CveCwe;
import com.tup.reconac.modules.vulnEnum.models.Cwe;
import com.tup.reconac.modules.vulnEnum.models.Referencia;
import com.tup.reconac.modules.vulnEnum.repositories.CpeCveRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CpeRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CveCweRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CveRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CweRepository;
import com.tup.reconac.modules.vulnEnum.repositories.ReferenciaRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheService {

    @Value("${cache.ttl-hours}")
    private long ttlHours;

    private final Clock clock;
    private final RedisTemplate<String, Object> redis;
    private final CpeRepository cpeRepository;
    private final CveRepository cveRepository;
    private final CweRepository cweRepository;
    private final CpeCveRepository cpeCveRepository;
    private final CveCweRepository cveCweRepository;
    private final ReferenciaRepository referenciaRepository;

    private static final String PREFIX_CPE = "cache:cpe:";
    private static final String PREFIX_CVE = "cache:cve:";
    private static final String PREFIX_CWE = "cache:cwe:";
    private static final String PREFIX_CPE_CVE_BY_CPE = "cache:cpe_cve:cpe:";
    private static final String PREFIX_CPE_CVE_BY_CVE = "cache:cpe_cve:cve:";
    private static final String PREFIX_CVE_CWE_BY_CVE = "cache:cve_cwe:cve:";
    private static final String PREFIX_CVE_CWE_BY_CWE = "cache:cve_cwe:cwe:";
    private static final String PREFIX_REFERENCIA_BY_CVE = "cache:referencia:cve:";

    // ========================
    // CPE
    // ========================

    public Optional<Cpe> getCpe(String uri) {
        Cpe cached = getFromCache(PREFIX_CPE, uri, Cpe.class);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Cpe> fromDb = cpeRepository.findByUri(uri);
        fromDb.ifPresent(cpe -> putInCache(PREFIX_CPE, uri, cpe));
        return fromDb;
    }

    public Cpe saveCpe(Cpe cpe) {
        Cpe saved = cpeRepository.save(cpe);
        putInCache(PREFIX_CPE, cpe.getUri(), saved);
        return saved;
    }

    public void invalidateCpe(String uri) {
        redis.delete(PREFIX_CPE + uri);
    }

    // ========================
    // CVE
    // ========================

    public Cve updateCveEpss(
            Cve cve,
            BigDecimal epss
    ) {
        cve.setEpss(epss);

        Cve saved = cveRepository.save(cve);

        putInCache(
                PREFIX_CVE,
                saved.getCve(),
                saved
        );

        return saved;
    }

    public Optional<Cve> getCve(String cveId) {
        Cve cached = getFromCache(PREFIX_CVE, cveId, Cve.class);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Cve> fromDb = cveRepository.findByCve(cveId);
        fromDb.ifPresent(cve -> putInCache(PREFIX_CVE, cveId, cve));
        return fromDb;
    }

    public Cve saveCve(Cve cve) {
        Cve saved = cveRepository.save(cve);
        putInCache(PREFIX_CVE, cve.getCve(), saved);
        return saved;
    }

    public void invalidateCve(String cveId) {
        redis.delete(PREFIX_CVE + cveId);
    }

    // ========================
    // CWE
    // ========================

    public Optional<Cwe> getCwe(String cweCode) {
        Cwe cached = getFromCache(PREFIX_CWE, cweCode, Cwe.class);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Cwe> fromDb = cweRepository.findByCweCode(cweCode);
        fromDb.ifPresent(cwe -> putInCache(PREFIX_CWE, cweCode, cwe));
        return fromDb;
    }

    public Cwe saveCwe(Cwe cwe) {
        Cwe saved = cweRepository.save(cwe);
        putInCache(PREFIX_CWE, cwe.getCweCode(), saved);
        return saved;
    }

    public void invalidateCwe(String cweCode) {
        redis.delete(PREFIX_CWE + cweCode);
    }

    // ========================
    // CPE_CVE
    // ========================

    public List<CpeCve> getCpeCveByCpeId(UUID cpeId) {
        String key = cpeId.toString();
        List<CpeCve> cached = getFromCache(PREFIX_CPE_CVE_BY_CPE, key, List.class);
        if (cached != null) {
            return cached;
        }
        List<CpeCve> fromDb = cpeCveRepository.findByCpeId(cpeId);
        putInCache(PREFIX_CPE_CVE_BY_CPE, key, fromDb);
        return fromDb;
    }

    public List<CpeCve> getCpeCveByCveId(UUID cveId) {
        String key = cveId.toString();
        List<CpeCve> cached = getFromCache(PREFIX_CPE_CVE_BY_CVE, key, List.class);
        if (cached != null) {
            return cached;
        }
        List<CpeCve> fromDb = cpeCveRepository.findByCveId(cveId);
        putInCache(PREFIX_CPE_CVE_BY_CVE, key, fromDb);
        return fromDb;
    }

    public void invalidateCpeCve(UUID cpeId, UUID cveId) {
        redis.delete(PREFIX_CPE_CVE_BY_CPE + cpeId);
        redis.delete(PREFIX_CPE_CVE_BY_CVE + cveId);
    }

    // ========================
    // CVE_CWE
    // ========================

    public List<CveCwe> getCveCweByCveId(UUID cveId) {
        String key = cveId.toString();
        List<CveCwe> cached = getFromCache(PREFIX_CVE_CWE_BY_CVE, key, List.class);
        if (cached != null) {
            return cached;
        }
        List<CveCwe> fromDb = cveCweRepository.findByCveId(cveId);
        putInCache(PREFIX_CVE_CWE_BY_CVE, key, fromDb);
        return fromDb;
    }

    public List<CveCwe> getCveCweByCweId(UUID cweId) {
        String key = cweId.toString();
        List<CveCwe> cached = getFromCache(PREFIX_CVE_CWE_BY_CWE, key, List.class);
        if (cached != null) {
            return cached;
        }
        List<CveCwe> fromDb = cveCweRepository.findByCweId(cweId);
        putInCache(PREFIX_CVE_CWE_BY_CWE, key, fromDb);
        return fromDb;
    }

    public void invalidateCveCwe(UUID cveId, UUID cweId) {
        redis.delete(PREFIX_CVE_CWE_BY_CVE + cveId);
        redis.delete(PREFIX_CVE_CWE_BY_CWE + cweId);
    }

    // ========================
    // REFERENCIA
    // ========================

    public List<Referencia> getReferenciasByCveId(UUID cveId) {
        String key = cveId.toString();
        List<Referencia> cached = getFromCache(PREFIX_REFERENCIA_BY_CVE, key, List.class);
        if (cached != null) {
            return cached;
        }
        List<Referencia> fromDb = referenciaRepository.findByCveId(cveId);
        putInCache(PREFIX_REFERENCIA_BY_CVE, key, fromDb);
        return fromDb;
    }

    public void invalidateReferencia(UUID cveId) {
        redis.delete(PREFIX_REFERENCIA_BY_CVE + cveId);
    }

    // ========================
    // Guard: ultimo_check
    // ========================

    public boolean shouldSkipExternalQuery(Cpe cpe) {
        if (cpe == null || cpe.getUltimoCheck() == null) {
            return false;
        }
        return cpe.getUltimoCheck().plus(Duration.ofHours(ttlHours)).isAfter(LocalDateTime.now(clock));
    }

    // ========================
    // Internal helpers
    // ========================

    @SuppressWarnings("unchecked")
    private <T> T getFromCache(String prefix, String key, Class<T> type) {
        Object value = redis.opsForValue().get(prefix + key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    private void putInCache(String prefix, String key, Object value) {
        redis.opsForValue().set(prefix + key, value, Duration.ofHours(ttlHours));
    }
}
