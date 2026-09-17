package com.tup.reconac.feature.auditoria.services;

import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.domain.AuditoriaConsultService;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaDeleteService;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.feature.escaneo.repositories.EscaneoRepository;
import com.tup.reconac.feature.escaneo.services.EscaneoDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditoriaDeleteService
        implements IAuditoriaDeleteService {

    private final AuditoriaRepository auditoriaRepository;
    private final EscaneoRepository escaneoRepository;
    private final EscaneoDeleteService escaneoDeleteService;
    private final AuditoriaConsultService auditoriaConsultService;

    @Override
    @Transactional
    public void deleteById(UUID auditoriaId) {

        /*
         * Valida:
         * - que exista
         * - que pertenezca al usuario autenticado
         */
        auditoriaConsultService.verifyAuditoriaOwnership(
                auditoriaId
        );

        /*
         * Ya tenés este método en EscaneoRepository.
         */
        List<Escaneo> escaneos =
                escaneoRepository.findAllByAuditoriaId(
                        auditoriaId
                );

        /*
         * Cada EscaneoDeleteService se ocupa de:
         *
         * puerto_cpe
         *   ↓
         * puerto
         *   ↓
         * activo
         *   ↓
         * escaneo
         */
        for (Escaneo escaneo : escaneos) {
            escaneoDeleteService.eliminar(
                    escaneo.getId()
            );
        }

        /*
         * Los delete(escaneo) anteriores pueden estar
         * pendientes en el persistence context.
         *
         * Antes de borrar auditoria forzamos su ejecución
         * para satisfacer fk_escaneo_auditoria.
         */
        escaneoRepository.flush();

        /*
         * Ya no existen escaneos que referencien
         * la auditoría.
         */
        auditoriaRepository.deleteById(
                auditoriaId
        );
    }
}
