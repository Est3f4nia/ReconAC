package com.tup.reconac.feature.auditoria;

import com.tup.reconac.exceptions.auditoria.AuditoriaNotFoundException;
import com.tup.reconac.feature.auditoria.dtos.request.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.models.Auditoria;
import com.tup.reconac.feature.auditoria.repositories.AuditoriaRepository;
import com.tup.reconac.feature.auditoria.services.AuditoriaCreateService;
import com.tup.reconac.feature.auditoria.services.AuditoriaDeleteService;
import com.tup.reconac.feature.auditoria.services.AuditoriaGetService;
import com.tup.reconac.feature.auditoria.services.AuditoriaUpdateService;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.services.domain.CustomUserDetailsService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auditoria Services - Tests unitarios")
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private CustomUserDetailsService userService;

    @InjectMocks
    private AuditoriaCreateService createService;

    @InjectMocks
    private AuditoriaGetService getService;

    @InjectMocks
    private AuditoriaUpdateService updateService;

    @InjectMocks
    private AuditoriaDeleteService deleteService;

    private Auditoria auditoria;
    private AuditoriaRequestDto requestDto;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        auditoria = new Auditoria();
        auditoria.setId(UUID.randomUUID());
        auditoria.setUsuarioId(UUID.randomUUID());
        auditoria.setNombre("Test Audit");
        auditoria.setObjetivo("192.168.1.1");
        auditoria.setFechaGeneracion(LocalDateTime.now());

        usuario = new Usuario();
        usuario.setId(auditoria.getUsuarioId());
        usuario.setEmail("test@test.com");

        requestDto = new AuditoriaRequestDto(
                "Test Audit",
                "192.168.1.1"
        );
    }

    // ========================
    // CREATE
    // ========================

    @Test
    @DisplayName("Create - Happy path: crea auditoría correctamente")
    void create_whenValidData_savesAndReturnsResponse() {
        when(userService.getAuthenticatedUser()).thenReturn(usuario);
        when(auditoriaRepository.save(any(Auditoria.class))).thenReturn(auditoria);

        AuditoriaResponse response = createService.create(requestDto);

        assertNotNull(response);
        assertEquals("Test Audit", response.nombre());
        verify(auditoriaRepository).save(any(Auditoria.class));
    }

    @Test
    @DisplayName("Create - Happy path: nombre vacío se asigna por defecto")
    void create_whenEmptyNombre_setsDefaultName() {
        AuditoriaRequestDto emptyNameRequest = new AuditoriaRequestDto(
                "",
                "192.168.1.1"
        );

        when(userService.getAuthenticatedUser()).thenReturn(usuario);
        when(auditoriaRepository.save(any(Auditoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditoriaResponse response =
                createService.create(emptyNameRequest);

        ArgumentCaptor<Auditoria> captor =
                ArgumentCaptor.forClass(Auditoria.class);

        verify(auditoriaRepository).save(captor.capture());

        Auditoria saved = captor.getValue();

        assertEquals("Nueva Auditoría", saved.getNombre());

        assertNotNull(response);
        assertEquals("Nueva Auditoría", response.nombre());
    }

    // ========================
    // GET
    // ========================

    @Test
    @DisplayName("GetAll - Happy path: retorna lista de auditorías")
    void getAll_whenAuditoriasExist_returnsList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Auditoria> page = new PageImpl<>(List.of(auditoria));
        when(auditoriaRepository.findAll(pageable)).thenReturn(page);

        Page<AuditoriaResponse> response = getService.getAll(pageable);

        assertEquals(1, response.getContent().size());
        assertEquals("Test Audit", response.getContent().getFirst().nombre());
    }

    @Test
    @DisplayName("GetAll - Empty: retorna lista vacía")
    void getAll_whenNoAuditorias_returnsEmptyList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Auditoria> page = new PageImpl<>(List.of());
        when(auditoriaRepository.findAll(pageable)).thenReturn(page);

        Page<AuditoriaResponse> response = getService.getAll(pageable);

        assertTrue(response.getContent().isEmpty());
    }

    // ========================
    // UPDATE
    // ========================

    @Test
    @DisplayName("Update - Happy path: actualiza auditoría existente")
    void update_whenExists_updatesAndReturnsResponse() {
        when(auditoriaRepository.findById(auditoria.getId())).thenReturn(Optional.of(auditoria));

        AuditoriaResponse response = updateService.update(requestDto, auditoria.getId());

        assertNotNull(response);
        assertEquals("Test Audit", response.nombre());
        // No se llama repo.save() explícitamente — dirty checking en @Transactional
    }

    @Test
    @DisplayName("Update - Error: auditoría no encontrada")
    void update_whenNotExists_throwsAuditoriaNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(auditoriaRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(AuditoriaNotFoundException.class,
                () -> updateService.update(requestDto, nonExistentId));
    }

    // ========================
    // DELETE
    // ========================

    @Test
    @DisplayName("Delete - Happy path: elimina auditoría existente")
    void delete_whenExists_deletesSuccessfully() {
        when(auditoriaRepository.existsById(auditoria.getId())).thenReturn(true);

        assertDoesNotThrow(() -> deleteService.deleteById(auditoria.getId()));

        verify(auditoriaRepository).deleteById(auditoria.getId());
    }

    @Test
    @DisplayName("Delete - Error: auditoría no encontrada")
    void delete_whenNotExists_throwsAuditoriaNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(auditoriaRepository.existsById(nonExistentId)).thenReturn(false);

        assertThrows(AuditoriaNotFoundException.class,
                () -> deleteService.deleteById(nonExistentId));

        verify(auditoriaRepository, never()).deleteById(any());
    }
}
