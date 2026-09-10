package com.tup.reconac.modules.vulnEnum;

import com.tup.reconac.cache.CacheService;
import com.tup.reconac.modules.vulnEnum.mappers.CpeParser;
import com.tup.reconac.modules.vulnEnum.mappers.NvdVulnerabilityMapper;
import com.tup.reconac.modules.vulnEnum.models.*;
import com.tup.reconac.modules.vulnEnum.repositories.*;
import com.tup.reconac.modules.vulnEnum.dtos.nvd.*;
import com.tup.reconac.modules.vulnEnum.services.cache.VulnerabilityPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CpePersistenceRegressionTest {
    @Test void parsesNmapExamplesAndMissingVersions() {
        assertEquals(new CpeParser.Parts("openbsd", "openssh", "6.6.1p1"),
                CpeParser.parse("cpe:/a:openbsd:openssh:6.6.1p1"));
        assertEquals(new CpeParser.Parts("igor_sysoev", "nginx", null),
                CpeParser.parse("cpe:/a:igor_sysoev:nginx"));
        assertEquals(new CpeParser.Parts("linux", "linux_kernel", null),
                CpeParser.parse("cpe:/o:linux:linux_kernel"));
        assertEquals(new CpeParser.Parts("apache", "http_server", "2.4.7"),
                CpeParser.parse("cpe:/a:apache:http_server:2.4.7"));
    }

    @Test void decodesUriAndFormattedBindingWithoutSplittingEscapedColon() {
        assertEquals("foo:bar", CpeParser.parse("cpe:/a:vendor:foo%3Abar:1+2").product());
        assertEquals("1+2", CpeParser.parse("cpe:/a:vendor:foo%3Abar:1+2").version());
        assertEquals("foo:bar", CpeParser.parse("cpe:2.3:a:vendor:foo\\:bar:*:*:*:*:*:*:*:*").product());
        assertNull(CpeParser.parse("cpe:2.3:a:vendor:product:*:*:*:*:*:*:*:*").version());
        assertThrows(IllegalArgumentException.class, () -> CpeParser.parse("invalid"));
    }

    @Test void mapsNvdDescriptionSeverityScoreDatesCwesAndReferences() {
        var data = sample();
        assertEquals("Example vulnerability", data.description());
        assertEquals("HIGH", data.severity());
        assertEquals(0, new BigDecimal("7.5").compareTo(data.cvssScore()));
        assertEquals(LocalDateTime.of(2026, 9, 8, 12, 0), data.publishedDate());
        assertEquals(List.of("CWE-79"), data.cwes());
        assertEquals(1, data.references().size());
    }

    @Test void enrichesExistingRowsAndPersistsRelationsWithoutOverwritingEpssKev() {
        var cpes = mock(CpeRepository.class);
        var cves = mock(CveRepository.class);
        var cwes = mock(CweRepository.class);
        var links = mock(CpeCveRepository.class);
        var weaknesses = mock(CveCweRepository.class);
        var refs = mock(ReferenciaRepository.class);
        var service = new VulnerabilityPersistenceService(mock(RedisTemplate.class),
                JsonMapper.builder().build(), cpes, cves, cwes, links, weaknesses, refs,
                mock(com.tup.reconac.modules.vulnEnum.services.VulnerabilityCatalogLock.class));
        var cpe = new Cpe();
        cpe.setId(UUID.randomUUID());
        cpe.setUri("cpe:/a:openbsd:openssh:6.6.1p1");
        var cve = new Cve();
        cve.setId(UUID.randomUUID());
        cve.setEpss(new BigDecimal("0.0074"));
        cve.setKev(true);
        when(cpes.findByUri(cpe.getUri())).thenReturn(Optional.of(cpe));
        when(cves.findByCve("CVE-2026-44185")).thenReturn(Optional.of(cve));
        var cwe = new Cwe();
        cwe.setId(UUID.randomUUID());
        when(cwes.findByCweCode("CWE-79")).thenReturn(Optional.of(cwe));
        var entry = new NvdCacheEntry(cpe.getUri(), LocalDateTime.now(), List.of(sample()));
        service.persist(entry);
        assertEquals("openbsd", cpe.getVendor());
        assertEquals("openssh", cpe.getProducto());
        assertEquals("6.6.1p1", cpe.getVersion());
        assertEquals("HIGH", cve.getSeveridad());
        assertEquals(new BigDecimal("0.0074"), cve.getEpss());
        assertTrue(cve.getKev());
        verify(links).save(any(CpeCve.class));
        verify(weaknesses).save(any(CveCwe.class));
        verify(refs).save(any(Referencia.class));
        when(links.findByCpeIdAndCveId(cpe.getId(), cve.getId()))
                .thenReturn(Optional.of(new CpeCve(cpe.getId(), cve.getId())));
        service.persist(entry);
        verify(links, times(1)).save(any(CpeCve.class));
    }

    private NvdVulnerabilityData sample() {
        return new NvdVulnerabilityMapper().map(JsonMapper.builder().build().readTree("""
            {"id":"CVE-2026-44185","descriptions":[{"lang":"en","value":"Example vulnerability"}],
             "published":"2026-09-08T12:00:00.000","lastModified":"2026-09-09T12:00:00.000",
             "metrics":{"cvssMetricV31":[{"cvssData":{"baseScore":7.5,"baseSeverity":"HIGH",
                  "vectorString":"CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"}}]},
             "weaknesses":[{"description":[{"lang":"en","value":"CWE-79"}]}],
             "references":[{"url":"https://example.com/advisory","source":"vendor","tags":["Patch"]}]}
            """));
    }
}
