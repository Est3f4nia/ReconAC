package com.tup.reconac.feature.escaneo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.ModulesConfig;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.List;
import java.util.UUID;

@Tag(name = "Escaneos")
@RestController
@RequestMapping("/api/auditorias/{auditoriaId}/escaneos")
@RequiredArgsConstructor
public class ScanLogsController {
    private final EscaneoConsultService escaneos;
    private final ModulesConfig modules;
    private final RestClient.Builder clients;

    @Operation(summary = "Leer salida incremental del módulo", description = "Consulta logs por ID persistido del escaneo. available=false indica que el módulo o la salida no están disponibles; sigue siendo HTTP 200. Los logs son temporales y se pierden al reiniciar Python.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
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
    public record Logs(
            @Schema(description = "false si la salida expiró, el módulo reinició o no pudo consultarse") boolean available,
            List<Line> lines,
            @Schema(description = "Cursor que se debe enviar como after en la próxima consulta") long nextCursor,
            @Schema(description = "Se descartaron líneas anteriores al cursor por el límite del buffer") boolean truncated) {
        static Logs unavailable(long after) { return new Logs(false, List.of(), Math.max(0, after), false); }
    }
}
