package com.tup.reconac.modules.vulnEnum;

import com.tup.reconac.modules.vulnEnum.services.MetricasAuditoriaService;
import com.tup.reconac.modules.vulnEnum.repositories.CveRepository;
import com.tup.reconac.modules.vulnEnum.models.Cve;
import com.tup.reconac.feature.escaneo.models.*;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.puerto.repositories.PuertoRepository;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class DashboardContractTest {
    @Test void readsPerCpeSnapshotsAndKeepsDifferentScanFiltersAndHostAttribution() {
        var scans = mock(EscaneoRepository.class);
        var cves = mock(CveRepository.class);
        var ownership = mock(AuditoriaConsultService.class);
        var service = new MetricasAuditoriaService(scans, mock(ActivoRepository.class),
                mock(PuertoRepository.class), null, null, JsonMapper.builder().build(), cves, ownership);
        var audit = UUID.randomUUID();
        Escaneo first = scan("""
            {"hosts":[{"ip":"192.0.2.1","puertos":[{"cpes":["cpe:/a:v:p:1"]}]},
                      {"ip":"192.0.2.2","puertos":[{"cpes":["cpe:/a:v:q:1"]}]}],
             "apiResults":{"cpe:/a:v:p:1":{"vulnerabilities":[{"cve_id":"CVE-2026-1000","cvss_score":9.8}]},
                           "cpe:/a:v:q:1":{"vulnerabilities":[{"cve_id":"CVE-2026-2000","cvss_score":5.0}]}}}
            """);
        Escaneo second = scan("""
            {"hosts":[{"ip":"192.0.2.1","puertos":[{"cpes":["cpe:/a:v:p:1"]}]}],
             "apiResults":{"cpe:/a:v:p:1":{"vulnerabilities":[]}}}
            """);
        when(scans.findByAuditoriaIdOrderByCreadoADesc(audit)).thenReturn(List.of(first, second));
        var stored = new Cve();
        stored.setCve("CVE-2026-1000"); stored.setEpss(new BigDecimal("0.15")); stored.setKev(true);
        when(cves.findAllByCveIn(any())).thenReturn(List.of(stored));
        var result = service.getDashboard(audit);
        assertEquals(2, result.kpis().cves());
        assertEquals(2, result.historial().get(0).cves());
        assertEquals(0, result.historial().get(1).cves());
        var hosts = result.hosts().masVulnerabilidadesCriticas();
        assertEquals("192.0.2.1", hosts.get(0).ip());
        assertEquals(1, hosts.get(0).vulnerabilidades());
        assertEquals(1, hosts.get(0).vulnerabilidadesCriticas());
        assertEquals(1, hosts.get(1).vulnerabilidades());
        verify(ownership).verifyAuditoriaOwnership(audit);
    }

    @Test void invalidSnapshotIsAnErrorRatherThanFalseZeroMetrics() {
        var scans = mock(EscaneoRepository.class);
        var audit = UUID.randomUUID();
        when(scans.findByAuditoriaIdOrderByCreadoADesc(audit)).thenReturn(List.of(scan("{invalid")));
        var service = new MetricasAuditoriaService(scans, mock(ActivoRepository.class),
                mock(PuertoRepository.class), null, null, JsonMapper.builder().build(),
                mock(CveRepository.class), mock(AuditoriaConsultService.class));
        assertThrows(IllegalStateException.class, () -> service.getDashboard(audit));
    }

    private Escaneo scan(String json) {
        var scan = new Escaneo();
        scan.setId(UUID.randomUUID()); scan.setEstado(EscaneoEstado.COMPLETADO);
        scan.setCreadoA(LocalDateTime.now()); scan.setResultado(json);
        return scan;
    }
}
