package com.tup.reconac.modules.vulnEnum;

import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.models.CpeCve;
import com.tup.reconac.modules.vulnEnum.models.PuertoCpe;
import com.tup.reconac.modules.vulnEnum.repositories.CpeCveRepository;
import com.tup.reconac.modules.vulnEnum.repositories.CpeRepository;
import com.tup.reconac.modules.vulnEnum.repositories.PuertoCpeRepository;
import com.tup.reconac.modules.vulnEnum.services.metricas.CveDesgloseService;
import com.tup.reconac.modules.vulnEnum.services.metricas.HostDesgloseService;
import com.tup.reconac.modules.vulnEnum.services.metricas.MetricasAuditoriaService;
import com.tup.reconac.modules.vulnEnum.repositories.CveRepository;
import com.tup.reconac.modules.vulnEnum.models.Cve;
import com.tup.reconac.feature.escaneo.models.*;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.puerto.repositories.PuertoRepository;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.modules.vulnEnum.services.metricas.ScanMetricsService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DashboardContractTest {

    @Test
    void readsPerCpeSnapshotsAndKeepsDifferentScanFiltersAndHostAttribution() {

        EscaneoRepository scans = mock(EscaneoRepository.class);
        ActivoRepository activos = mock(ActivoRepository.class);
        PuertoRepository puertos = mock(PuertoRepository.class);
        CveRepository cves = mock(CveRepository.class);
        CpeRepository cpes = mock(CpeRepository.class);
        PuertoCpeRepository puertoCpes = mock(PuertoCpeRepository.class);
        CpeCveRepository cpeCves = mock(CpeCveRepository.class);
        AuditoriaConsultService ownership = mock(AuditoriaConsultService.class);

        ObjectMapper objectMapper = JsonMapper.builder().build();

        ScanMetricsService scanMetricsService =
                new ScanMetricsService(objectMapper, cves);

        CveDesgloseService cveDesgloseService =
                new CveDesgloseService(
                        puertoCpes,
                        cpeCves,
                        cves
                );

        HostDesgloseService hostDesgloseService =
                new HostDesgloseService(
                        puertoCpes,
                        cpes,
                        cpeCves,
                        cves
                );

        MetricasAuditoriaService service =
                new MetricasAuditoriaService(
                        scans,
                        activos,
                        puertos,
                        ownership,
                        scanMetricsService,
                        cveDesgloseService,
                        hostDesgloseService
                );

        UUID audit = UUID.randomUUID();

        Escaneo first = scan("""
                {
                  "hosts": [
                    {
                      "ip": "192.0.2.1",
                      "puertos": [
                        {"cpes": ["cpe:/a:v:p:1"]}
                      ]
                    },
                    {
                      "ip": "192.0.2.2",
                      "puertos": [
                        {"cpes": ["cpe:/a:v:q:1"]}
                      ]
                    }
                  ],
                  "apiResults": {
                    "cpe:/a:v:p:1": {
                      "vulnerabilities": [
                        {
                          "cve_id": "CVE-2026-1000",
                          "cvss_score": 9.8
                        }
                      ]
                    },
                    "cpe:/a:v:q:1": {
                      "vulnerabilities": [
                        {
                          "cve_id": "CVE-2026-2000",
                          "cvss_score": 5.0
                        }
                      ]
                    }
                  }
                }
                """);

        Escaneo second = scan("""
                {
                  "hosts": [
                    {
                      "ip": "192.0.2.1",
                      "puertos": [
                        {"cpes": ["cpe:/a:v:p:1"]}
                      ]
                    }
                  ],
                  "apiResults": {
                    "cpe:/a:v:p:1": {
                      "vulnerabilities": []
                    }
                  }
                }
                """);

        when(scans.findByAuditoriaIdOrderByCreadoADesc(audit))
                .thenReturn(List.of(first, second));

        /*
         * ========================================================
         * Activos
         * ========================================================
         */

        UUID activo1Id = UUID.randomUUID();
        UUID activo2Id = UUID.randomUUID();
        UUID activo3Id = UUID.randomUUID();

        Activo activo1 = mock(Activo.class);
        when(activo1.getId()).thenReturn(activo1Id);
        when(activo1.getEscaneoId()).thenReturn(first.getId());
        when(activo1.getHost()).thenReturn("192.0.2.1");

        Activo activo2 = mock(Activo.class);
        when(activo2.getId()).thenReturn(activo2Id);
        when(activo2.getEscaneoId()).thenReturn(first.getId());
        when(activo2.getHost()).thenReturn("192.0.2.2");

        Activo activo3 = mock(Activo.class);
        when(activo3.getId()).thenReturn(activo3Id);
        when(activo3.getEscaneoId()).thenReturn(second.getId());
        when(activo3.getHost()).thenReturn("192.0.2.1");

        when(activos.findByEscaneoIdIn(any()))
                .thenReturn(List.of(
                        activo1,
                        activo2,
                        activo3
                ));

        /*
         * ========================================================
         * Puertos
         * ========================================================
         */

        UUID puerto1Id = UUID.randomUUID();
        UUID puerto2Id = UUID.randomUUID();
        UUID puerto3Id = UUID.randomUUID();

        Puerto puerto1 = mock(Puerto.class);
        when(puerto1.getId()).thenReturn(puerto1Id);
        when(puerto1.getActivoId()).thenReturn(activo1Id);

        Puerto puerto2 = mock(Puerto.class);
        when(puerto2.getId()).thenReturn(puerto2Id);
        when(puerto2.getActivoId()).thenReturn(activo2Id);

        Puerto puerto3 = mock(Puerto.class);
        when(puerto3.getId()).thenReturn(puerto3Id);
        when(puerto3.getActivoId()).thenReturn(activo3Id);

        when(puertos.findByActivoIdIn(any()))
                .thenReturn(List.of(
                        puerto1,
                        puerto2,
                        puerto3
                ));

        /*
         * ========================================================
         * CPEs
         * ========================================================
         */

        UUID cpePId = UUID.randomUUID();
        UUID cpeQId = UUID.randomUUID();

        Cpe cpeP = mock(Cpe.class);
        when(cpeP.getId()).thenReturn(cpePId);
        when(cpeP.getUri()).thenReturn("cpe:/a:v:p:1");

        Cpe cpeQ = mock(Cpe.class);
        when(cpeQ.getId()).thenReturn(cpeQId);
        when(cpeQ.getUri()).thenReturn("cpe:/a:v:q:1");

        PuertoCpe puertoCpe1 = mock(PuertoCpe.class);
        when(puertoCpe1.getPuertoId()).thenReturn(puerto1Id);
        when(puertoCpe1.getCpeId()).thenReturn(cpePId);

        PuertoCpe puertoCpe2 = mock(PuertoCpe.class);
        when(puertoCpe2.getPuertoId()).thenReturn(puerto2Id);
        when(puertoCpe2.getCpeId()).thenReturn(cpeQId);

        PuertoCpe puertoCpe3 = mock(PuertoCpe.class);
        when(puertoCpe3.getPuertoId()).thenReturn(puerto3Id);
        when(puertoCpe3.getCpeId()).thenReturn(cpePId);

        when(puertoCpes.findByPuertoIdIn(any()))
                .thenReturn(List.of(
                        puertoCpe1,
                        puertoCpe2,
                        puertoCpe3
                ));

        when(cpes.findAllById(any()))
                .thenReturn(List.of(
                        cpeP,
                        cpeQ
                ));

        /*
         * ========================================================
         * CVEs
         * ========================================================
         */

        UUID cve1000Id = UUID.randomUUID();
        UUID cve2000Id = UUID.randomUUID();

        Cve cve1000 = mock(Cve.class);
        when(cve1000.getId()).thenReturn(cve1000Id);
        when(cve1000.getCve()).thenReturn("CVE-2026-1000");
        when(cve1000.getCvss()).thenReturn(new BigDecimal("9.8"));
        when(cve1000.getEpss()).thenReturn(new BigDecimal("0.15"));
        when(cve1000.getKev()).thenReturn(true);

        Cve cve2000 = mock(Cve.class);
        when(cve2000.getId()).thenReturn(cve2000Id);
        when(cve2000.getCve()).thenReturn("CVE-2026-2000");
        when(cve2000.getCvss()).thenReturn(new BigDecimal("5.0"));
        when(cve2000.getKev()).thenReturn(false);

        when(cves.findAllByCveIn(any()))
                .thenReturn(List.of(
                        cve1000,
                        cve2000
                ));

        when(cves.findAllById(any()))
                .thenReturn(List.of(
                        cve1000,
                        cve2000
                ));

        CpeCve cpeCve1 = mock(CpeCve.class);
        when(cpeCve1.getCpeId()).thenReturn(cpePId);
        when(cpeCve1.getCveId()).thenReturn(cve1000Id);

        CpeCve cpeCve2 = mock(CpeCve.class);
        when(cpeCve2.getCpeId()).thenReturn(cpeQId);
        when(cpeCve2.getCveId()).thenReturn(cve2000Id);

        when(cpeCves.findByCpeIdIn(any()))
                .thenReturn(List.of(
                        cpeCve1,
                        cpeCve2
                ));

        /*
         * ========================================================
         * Ejecución
         * ========================================================
         */

        var result =
                service.getDashboard(audit);

        /*
         * El primer snapshot contiene dos CVEs.
         * El segundo no contiene ninguna.
         */
        assertEquals(
                2,
                result.kpis().cves()
        );

        assertEquals(
                2,
                result.historial().get(0).cves()
        );

        assertEquals(
                0,
                result.historial().get(1).cves()
        );

        /*
         * Solo 192.0.2.1 posee una CVE crítica.
         */
        var criticos =
                result.hosts().masVulnerabilidadesCriticas();

        assertEquals(1, criticos.size());

        assertEquals(
                "192.0.2.1",
                criticos.getFirst().ip()
        );

        assertEquals(
                1,
                criticos.getFirst().vulnerabilidades()
        );

        assertEquals(
                1,
                criticos.getFirst().vulnerabilidadesCriticas()
        );

        /*
         * El ranking de riesgo contiene ambos hosts
         * y conserva la atribución de cada CVE.
         */
        var riesgo =
                result.hosts().mayorRiesgoExplotacion();

        assertEquals(2, riesgo.size());

        assertEquals(
                "192.0.2.1",
                riesgo.get(0).ip()
        );

        assertEquals(
                1,
                riesgo.get(0).vulnerabilidades()
        );

        assertEquals(
                "192.0.2.2",
                riesgo.get(1).ip()
        );

        assertEquals(
                1,
                riesgo.get(1).vulnerabilidades()
        );

        verify(ownership)
                .verifyAuditoriaOwnership(audit);
    }

    @Test
    void invalidSnapshotIsAnErrorRatherThanFalseZeroMetrics() {

        EscaneoRepository scans =
                mock(EscaneoRepository.class);

        ActivoRepository activos =
                mock(ActivoRepository.class);

        PuertoRepository puertos =
                mock(PuertoRepository.class);

        CveRepository cves =
                mock(CveRepository.class);

        PuertoCpeRepository puertoCpes =
                mock(PuertoCpeRepository.class);

        CpeRepository cpes =
                mock(CpeRepository.class);

        CpeCveRepository cpeCves =
                mock(CpeCveRepository.class);

        AuditoriaConsultService ownership =
                mock(AuditoriaConsultService.class);

        UUID audit =
                UUID.randomUUID();

        when(scans.findByAuditoriaIdOrderByCreadoADesc(audit))
                .thenReturn(
                        List.of(
                                scan("{invalid")
                        )
                );

        when(activos.findByEscaneoIdIn(any()))
                .thenReturn(List.of());

        ObjectMapper objectMapper =
                JsonMapper.builder().build();

        ScanMetricsService scanMetricsService =
                new ScanMetricsService(
                        objectMapper,
                        cves
                );

        CveDesgloseService cveDesgloseService =
                new CveDesgloseService(
                        puertoCpes,
                        cpeCves,
                        cves
                );

        HostDesgloseService hostDesgloseService =
                new HostDesgloseService(
                        puertoCpes,
                        cpes,
                        cpeCves,
                        cves
                );

        MetricasAuditoriaService service =
                new MetricasAuditoriaService(
                        scans,
                        activos,
                        puertos,
                        ownership,
                        scanMetricsService,
                        cveDesgloseService,
                        hostDesgloseService
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.getDashboard(audit)
        );

        verify(ownership)
                .verifyAuditoriaOwnership(audit);
    }

    private Escaneo scan(String json) {

        Escaneo scan =
                new Escaneo();

        scan.setId(UUID.randomUUID());
        scan.setEstado(EscaneoEstado.COMPLETADO);
        scan.setCreadoA(LocalDateTime.now());
        scan.setResultado(json);

        return scan;
    }
}
