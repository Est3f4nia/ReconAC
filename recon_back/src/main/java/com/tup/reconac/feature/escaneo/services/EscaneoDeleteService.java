package com.tup.reconac.feature.escaneo.services;

import com.tup.reconac.exceptions.escaneo.EscaneoNotFoundException;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
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
    private final AuditoriaConsultService auditoriaConsultService;

    @Transactional
    public void eliminar(UUID escaneoId) {

        Escaneo escaneo = escaneoRepository.findById(escaneoId)
                .orElseThrow(() ->
                        new EscaneoNotFoundException(
                                "Escaneo no encontrado: " + escaneoId
                        )
                );

        /*
         * Verifica indirectamente que el escaneo
         * pertenezca a una auditoría del usuario autenticado.
         */
        auditoriaConsultService.verifyAuditoriaOwnership(
                escaneo.getAuditoriaId()
        );

        /*
         * escaneo
         *   └─ activo
         *       └─ puerto
         *           └─ puerto_cpe
         */

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

                /*
                 * 1. puerto_cpe depende de puerto
                 */
                puertoCpeRepository.deleteByPuertoIdIn(
                        puertoIds
                );

                /*
                 * Fuerza la eliminación antes del DELETE
                 * batch sobre puerto.
                 */
                puertoCpeRepository.flush();

                /*
                 * 2. puerto depende de activo
                 */
                puertoRepository.deleteAllByIdInBatch(
                        puertoIds
                );
            }

            /*
             * 3. activo depende de escaneo
             */
            activoRepository.deleteAllByIdInBatch(
                    activoIds
            );
        }

        /*
         * 4. finalmente, escaneo
         */
        escaneoRepository.delete(escaneo);
    }
}
