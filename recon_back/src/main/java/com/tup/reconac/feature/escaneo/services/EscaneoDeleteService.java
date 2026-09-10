package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import com.tup.reconac.feature.puerto.models.Puerto;
import com.tup.reconac.feature.puerto.repositories.PuertoRepository;
import com.tup.reconac.modules.vulnEnum.repositories.PuertoCpeRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EscaneoDeleteService {

    private final EscaneoRepository escaneoRepository;
    private final ActivoRepository activoRepository;
    private final PuertoRepository puertoRepository;
    private final PuertoCpeRepository puertoCpeRepository;

    @Transactional
    public void eliminar(UUID escaneoId) {

        // meter validaciones de ownership a la auditoria y al usuario

        Escaneo escaneo = escaneoRepository.findById(escaneoId)
                .orElseThrow(() ->
                        new EscaneoNotFoundException(
                                "Escaneo no encontrado (deleteservice)"
                        )
                );

        List<Activo> activos =
                activoRepository.findByEscaneoId(escaneoId);

        if (!activos.isEmpty()) {

            List<UUID> activoIds = activos.stream()
                    .map(Activo::getId)
                    .toList();

            List<Puerto> puertos =
                    puertoRepository.findByActivoIdIn(activoIds);

            if (!puertos.isEmpty()) {

                List<UUID> puertoIds = puertos.stream()
                        .map(Puerto::getId)
                        .toList();

                // puerto_cpe depende de puerto
                puertoCpeRepository.deleteByPuertoIdIn(puertoIds);

                // puerto depende de activo
                puertoRepository.deleteAllByIdInBatch(puertoIds);
            }

            // activo depende de escaneo
            activoRepository.deleteAllByIdInBatch(activoIds);
        }

        // Finalmente se puede eliminar el escaneo
        escaneoRepository.delete(escaneo);
    }
}
