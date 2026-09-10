package com.tup.reconac.modules.vulnEnum;
import com.tup.reconac.config.ModulesConfig;
import com.tup.reconac.feature.escaneo.controllers.ScanLogsController;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ScanLogsAuthorizationTest {
    @Test void rejectsUnauthorizedScanBeforeContactingPython() {
        var scans = mock(EscaneoConsultService.class);
        var modules = mock(ModulesConfig.class);
        var clients = mock(RestClient.Builder.class);
        var audit = UUID.randomUUID();
        var scan = UUID.randomUUID();
        when(scans.findEscaneoForAuditoria(audit, scan)).thenThrow(new IllegalArgumentException("Acceso denegado"));
        var controller = new ScanLogsController(scans, modules, clients);
        assertThrows(IllegalArgumentException.class, () -> controller.logs(audit, scan, 0));
        verifyNoInteractions(modules, clients);
    }
}
