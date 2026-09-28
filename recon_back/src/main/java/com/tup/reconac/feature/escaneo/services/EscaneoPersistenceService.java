package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.response.HostResult;
import com.tup.reconac.feature.escaneo.mappers.EscaneoResultMapper;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.puerto.dtos.PuertoResultadoResponse;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.feature.puerto.repositories.PuertoRepository;
import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.repositories.CpeRepository;
import com.tup.reconac.modules.vulnEnum.repositories.PuertoCpeRepository;
import com.tup.reconac.modules.vulnEnum.services.helpers.VulnerabilityCatalogLock;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Transacción aparte para persistir el escaneo.
 * Al crear es el escaneo E iniciar Python,
 * @Transactional no persistía el moduleJobId a tiempo para que las
 * actualizaciones de estado funcionen
 */

@Service
@AllArgsConstructor
public class EscaneoPersistenceService {

    private final EscaneoRepository repo;
    private final ActivoRepository activoRepo;
    private final PuertoRepository puertoRepo;
    private final AuditoriaConsultService auditoriaConsult;
    private final CpeRepository cpeRepo;
    private final PuertoCpeRepository puertoCpeRepo;
    private final VulnerabilityCatalogLock catalogLock;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Escaneo crear(UUID auditoriaId,  String[] objetivos,  String jobId) {
        auditoriaConsult.verifyAuditoriaOwnership(auditoriaId);

        Escaneo escaneo = new Escaneo();
        escaneo.setAuditoriaId(auditoriaId);
        escaneo.setObjetivos(objetivos);
        escaneo.setModuloJobId(jobId);
        escaneo.setEstado(EscaneoEstado.PENDIENTE);

        return repo.saveAndFlush(escaneo);
    }

    @Transactional
    public void completarCallback(String jobId, EscaneoResult result) {

        Escaneo escaneo = repo.findByModuloJobId(jobId).orElseThrow(() ->
                new EscaneoNotFoundException("Escaneo no encontrado para jobId: " + jobId));

        try {
            escaneo.setResultado(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el resultado del escaneo", e);
        }

        escaneo.setNmapVersion(result.nmapVersion());

        migrarActivos(escaneo, result.hosts());

        escaneo.setEstado(EscaneoEstado.COMPLETADO);
        escaneo.setProgreso(100);
        escaneo.setCompletadoA(LocalDateTime.now());
        escaneo.setMensajeError(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFalloCallback(String jobId, String mensaje) {

        repo.findByModuloJobId(jobId).ifPresent(escaneo -> {
            escaneo.setEstado(EscaneoEstado.FALLO);
            escaneo.setMensajeError(mensaje);
            escaneo.setCompletadoA(LocalDateTime.now());
        });
    }

    @Transactional
    public void actualizarStatus(String jobId, ScanStatusResponse status) {

        Escaneo escaneo = repo.findByModuloJobId(jobId).orElseThrow(() ->
                new EscaneoNotFoundException("Escaneo no encontrado para jobId: " + jobId));

        if (status.status() != null) escaneo.setEstado(status.status());
        if (status.progress() != null) escaneo.setProgreso(status.progress());
        if (status.error() != null) escaneo.setMensajeError(status.error());

        if (status.status() == EscaneoEstado.COMPLETADO || status.status() == EscaneoEstado.FALLO) {
            escaneo.setCompletadoA(LocalDateTime.now());
        }
    }

    private void migrarActivos(Escaneo escaneo, List<HostResult> hosts) {

        if (hosts == null || hosts.isEmpty()) return;

        for (HostResult host : hosts) {

            Activo activo = activoRepo.save(
                    EscaneoResultMapper.toActivo(escaneo.getId(), host)
            );

            if (host.puertos() == null || host.puertos().isEmpty()) continue;

            for (PuertoResultadoResponse result : host.puertos()) {

                Puerto puerto = puertoRepo.save(
                        EscaneoResultMapper.toPuerto(activo.getId(), result)
                );

                migrarCpes(puerto, result.cpes());
            }
        }
    }

    private void migrarCpes(Puerto puerto, List<String> cpes) {

        if (cpes == null || cpes.isEmpty()) return;

        catalogLock.acquire();

        for (String uri : cpes) {

            if (uri == null || uri.isBlank()) continue;

            Cpe cpe = cpeRepo.findByUri(uri)
                    .orElseGet(() ->
                            cpeRepo.save(EscaneoResultMapper.toCpe(uri)));

            if (!puertoCpeRepo.existsByPuertoIdAndCpeId(puerto.getId(), cpe.getId())) {
                puertoCpeRepo.save(
                        EscaneoResultMapper.toPuertoCpe(puerto.getId(), cpe.getId())
                );
            }
        }
    }
}
