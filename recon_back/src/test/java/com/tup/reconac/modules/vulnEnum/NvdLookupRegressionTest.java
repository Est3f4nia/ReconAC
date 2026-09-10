package com.tup.reconac.modules.vulnEnum;

import com.tup.reconac.cache.CacheService;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.repositories.UsuarioRepository;
import com.tup.reconac.feature.usuario.services.NvdApiKeyEncryptionService;
import com.tup.reconac.modules.vulnEnum.clients.NvdClient;
import com.tup.reconac.modules.vulnEnum.dtos.nvd.*;
import com.tup.reconac.modules.vulnEnum.mappers.NvdVulnerabilityMapper;
import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.repositories.*;
import com.tup.reconac.modules.vulnEnum.services.cache.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class NvdLookupRegressionTest {
    private final CacheService cache = mock(CacheService.class);
    private final NvdClient client = mock(NvdClient.class);
    private final VulnerabilityPersistenceService persistence = mock(VulnerabilityPersistenceService.class);
    private final EscaneoRepository scans = mock(EscaneoRepository.class);
    private final AuditoriaRepository audits = mock(AuditoriaRepository.class);
    private final UsuarioRepository users = mock(UsuarioRepository.class);
    private final NvdApiKeyEncryptionService keys = mock(NvdApiKeyEncryptionService.class);
    private final com.tup.reconac.modules.vulnEnum.services.EpssService epss =
            mock(com.tup.reconac.modules.vulnEnum.services.EpssService.class);
    private final com.tup.reconac.modules.vulnEnum.services.KevService kev =
            mock(com.tup.reconac.modules.vulnEnum.services.KevService.class);
    private final VulnerabilityService service = new VulnerabilityService(cache, client,
            new NvdVulnerabilityMapper(), persistence, scans, audits, users, keys,
            epss, kev);
    private final UUID scanId = UUID.randomUUID();
    private final String cpe = "cpe:/a:openbsd:openssh:6.6.1p1";

    @BeforeEach void setupOwner() {
        var scan = new Escaneo();
        scan.setAuditoriaId(UUID.randomUUID());
        var audit = new Auditoria();
        audit.setUsuarioId(UUID.randomUUID());
        var user = new Usuario();
        user.setNvdApiKey("encrypted-test-key");
        when(scans.findById(scanId)).thenReturn(Optional.of(scan));
        when(audits.findById(scan.getAuditoriaId())).thenReturn(Optional.of(audit));
        when(users.findById(audit.getUsuarioId())).thenReturn(Optional.of(user));
        when(keys.decrypt("encrypted-test-key")).thenReturn("test-key");
    }

    @Test void commitsEvenEmptySuccessfulLookupBeforeCaching() {
        when(client.findByCpe(cpe, "test-key")).thenReturn(List.of());
        assertTrue(service.lookup(request()).get(cpe).vulnerabilities().isEmpty());
        var order = inOrder(persistence, cache);
        order.verify(persistence).persist(any(NvdCacheEntry.class));
        order.verify(cache).putNvd(any(NvdCacheEntry.class));
    }

    @Test void databaseFailureCannotBecomeSuccessfulCacheHit() {
        when(client.findByCpe(cpe, "test-key")).thenReturn(List.of());
        doThrow(new IllegalStateException("database unavailable")).when(persistence).persist(any());
        assertThrows(IllegalStateException.class, () -> service.lookup(request()));
        verify(cache, never()).putNvd(any());
    }

    @Test void remoteFailureCannotBecomeEmptyPersistedLookup() {
        when(client.findByCpe(cpe, "test-key")).thenThrow(new IllegalStateException("NVD unavailable"));
        assertThrows(IllegalStateException.class, () -> service.lookup(request()));
        verifyNoInteractions(persistence);
        verify(cache, never()).putNvd(any());
    }

    private NvdLookupRequest request() {
        return new NvdLookupRequest(scanId, List.of(cpe), 0, BigDecimal.ZERO);
    }

    @Test void enrichmentRunsAfterPersistenceAndItsFailureKeepsNvdResult() {
        var raw = tools.jackson.databind.json.JsonMapper.builder().build().readTree("""
            {"id":"CVE-2026-1000","metrics":{"cvssMetricV31":[{"cvssData":{"baseScore":9.8,"baseSeverity":"CRITICAL"}}]}}
            """);
        when(client.findByCpe(cpe, "test-key")).thenReturn(List.of(raw));
        when(epss.enrichEpss(any())).thenThrow(new IllegalStateException("offline"));
        var result = service.lookup(request());
        assertEquals(1, result.get(cpe).vulnerabilities().size());
        var order = inOrder(persistence, epss, kev);
        order.verify(persistence).persist(any());
        order.verify(epss).enrichEpss(List.of("CVE-2026-1000"));
        order.verify(kev).enrichKev(List.of("CVE-2026-1000"));
    }
}
