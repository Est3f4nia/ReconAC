package com.tup.reconac.feature.escaneo.controllers;

import com.tup.reconac.config.ModulesConfig;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auditorias/{auditoriaId}/escaneos")
@RequiredArgsConstructor
public class ScanLogsController {
    private final EscaneoConsultService escaneos;
    private final ModulesConfig modules;
    private final RestClient.Builder clients;

    @GetMapping("/{escaneoId}/logs")
    public Logs logs(@PathVariable UUID auditoriaId, @PathVariable UUID escaneoId,
                     @RequestParam(defaultValue = "0") long after) {
        // Resolver por ID persistido y comprobar pertenencia antes de consultar el módulo.
        var scan = escaneos.findEscaneoForAuditoria(auditoriaId, escaneoId);
        if (scan.getModuloJobId() == null) return Logs.unavailable(after);
        try {
            var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
                    java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build());
            factory.setReadTimeout(java.time.Duration.ofSeconds(5));
            Logs result = clients.clone().requestFactory(factory).build().get()
                    .uri(modules.getEndpoint("recon").getUrl() + "/logs/{job}?after={after}",
                            scan.getModuloJobId(), Math.max(0, after))
                    .retrieve().body(Logs.class);
            return result == null ? Logs.unavailable(after) : result;
        } catch (RestClientException ex) {
            return Logs.unavailable(after);
        }
    }
    public record Line(long seq, String timestamp, String text) {}
    public record Logs(boolean available, List<Line> lines, long nextCursor, boolean truncated) {
        static Logs unavailable(long after) { return new Logs(false, List.of(), Math.max(0, after), false); }
    }
}
