package com.tup.reconac.feature.activo;

import com.tup.reconac.exceptions.activo.ActivoNotFoundException;
import com.tup.reconac.feature.activo.dtos.response.ActivoAgrupadoResponse;
import com.tup.reconac.feature.activo.dtos.request.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.response.ActivoResponse;
import com.tup.reconac.feature.activo.models.Activo;
import com.tup.reconac.feature.activo.repositories.ActivoRepository;
import com.tup.reconac.feature.activo.services.ActivoCreateService;
import com.tup.reconac.feature.activo.services.ActivoDeleteService;
import com.tup.reconac.feature.activo.services.ActivoGetService;
import com.tup.reconac.feature.activo.services.ActivoUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Activo Services - Tests unitarios")
class ActivoServiceTest {

    @Mock
    private ActivoRepository activoRepository;

    @InjectMocks
    private ActivoCreateService createService;

    @InjectMocks
    private ActivoGetService getService;

    @InjectMocks
    private ActivoUpdateService updateService;

    @InjectMocks
    private ActivoDeleteService deleteService;

    private Activo activo;
    private ActivoRequestDto requestDto;

    @BeforeEach
    void setUp() {
        activo = new Activo();
        activo.setId(UUID.randomUUID());
        activo.setEscaneoId(UUID.randomUUID());
        activo.setHost("192.168.1.10");
        activo.setHostname("webserver.local");
        activo.setSo("Linux");
        activo.setSoProbab(90);
        activo.setMac("AA:BB:CC:DD:EE:FF");
        activo.setDescripcion("Servidor web principal");

        requestDto = new ActivoRequestDto(
                activo.getEscaneoId(),
                "192.168.1.10",
                "webserver.local",
                "Linux",
                90,
                "AA:BB:CC:DD:EE:FF",
                "Servidor web principal"
        );
    }

    // ========================
    // CREATE
    // ========================

    @Test
    @DisplayName("Create - Happy path: crea activo correctamente")
    void create_whenValidData_savesAndReturnsResponse() {
        when(activoRepository.save(any(Activo.class))).thenReturn(activo);

        ActivoResponse response = createService.create(requestDto);

        // captura del objeto enviado para comprobar lógica del service
        ArgumentCaptor<Activo> captor = ArgumentCaptor.forClass(Activo.class);

        verify(activoRepository).save(captor.capture());

        Activo saved = captor.getValue();

        assertEquals(requestDto.host(), saved.getHost());
        assertEquals(requestDto.hostname(), saved.getHostname());
        assertEquals(requestDto.so(), saved.getSo());
        assertEquals(requestDto.soProbab(), saved.getSoProbab());
        assertEquals(requestDto.mac(), saved.getMac());
        assertEquals(requestDto.descripcion(), saved.getDescripcion());
        assertEquals(requestDto.escaneoId(), saved.getEscaneoId());

        assertNotNull(response);
        assertEquals(activo.getId(), response.id());
        assertEquals(activo.getHost(), response.host());
        assertEquals(activo.getHostname(), response.hostname());
        assertEquals(activo.getSo(), response.so());
        assertEquals(activo.getSoProbab(), response.soProbab());
        assertEquals(activo.getMac(), response.mac());
        assertEquals(activo.getDescripcion(), response.descripcion());
    }

    // ========================
    // GET
    // ========================

    @Test
    @DisplayName("GetAll - Happy path: retorna lista de activos")
    void getAll_whenActivosExist_returnsList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Activo> page = new PageImpl<>(List.of(activo));
        when(activoRepository.findAll(pageable)).thenReturn(page);

        Page<ActivoAgrupadoResponse> response = getService.getAll(pageable);

        assertEquals(1, response.getContent().size());
        assertEquals("192.168.1.10", response.getContent().getFirst().host());
        verify(activoRepository).findAll(pageable);
    }

    @Test
    @DisplayName("GetAll - Empty: retorna lista vacía")
    void getAll_whenNoActivos_returnsEmptyList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Activo> page = new PageImpl<>(List.of());
        when(activoRepository.findAll(pageable)).thenReturn(page);

        Page<ActivoAgrupadoResponse> response = getService.getAll(pageable);

        assertTrue(response.getContent().isEmpty());
        verify(activoRepository).findAll(pageable);
    }

    // ========================
    // UPDATE
    // ========================

    @Test
    @DisplayName("Update - Happy path: actualiza activo existente")
    void update_whenExists_updatesAndReturnsResponse() {
        activo.setHost("192.168.1.10");

        ActivoRequestDto updateDto = new ActivoRequestDto(
                activo.getEscaneoId(),
                "10.0.0.50",
                "nuevo-host",
                "Windows",
                75,
                "11:22:33:44:55:66",
                "Descripción modificada"
        );

        when(activoRepository.findById(activo.getId()))
                .thenReturn(Optional.of(activo));
        when(activoRepository.save(any(Activo.class))).thenReturn(activo);

        ActivoResponse response =
                updateService.update(updateDto, activo.getId());

        assertEquals("10.0.0.50", response.host());
        assertEquals("nuevo-host", response.hostname());
        assertEquals("Windows", response.so());
        assertEquals(75, response.soProbab());
        assertEquals("11:22:33:44:55:66", response.mac());
        assertEquals("Descripción modificada", response.descripcion());
        verify(activoRepository).save(activo);
    }

    @Test
    @DisplayName("Update - Error: activo no encontrado")
    void update_whenNotExists_throwsActivoNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(activoRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ActivoNotFoundException.class,
                () -> updateService.update(requestDto, nonExistentId));
        verify(activoRepository).findById(nonExistentId);
        verify(activoRepository, never()).save(any());
    }

    // ========================
    // DELETE
    // ========================

    @Test
    @DisplayName("Delete - Happy path: elimina activo existente")
    void delete_whenExists_deletesSuccessfully() {
        when(activoRepository.existsById(activo.getId())).thenReturn(true);

        assertDoesNotThrow(() -> deleteService.deleteById(activo.getId()));

        verify(activoRepository).existsById(activo.getId());
        verify(activoRepository).deleteById(activo.getId());
    }

    @Test
    @DisplayName("Delete - Error: activo no encontrado")
    void delete_whenNotExists_throwsActivoNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(activoRepository.existsById(nonExistentId)).thenReturn(false);

        assertThrows(ActivoNotFoundException.class,
                () -> deleteService.deleteById(nonExistentId));

        verify(activoRepository).existsById(nonExistentId);
        verify(activoRepository, never()).deleteById(any());
    }
}
