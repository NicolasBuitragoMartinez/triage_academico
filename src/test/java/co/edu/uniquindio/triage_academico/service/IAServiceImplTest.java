package co.edu.uniquindio.triage_academico.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.triage_academico.domain.SolicitudAcademica;
import co.edu.uniquindio.triage_academico.domain.enums.EstadoSolicitud;
import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import co.edu.uniquindio.triage_academico.dto.response.ResumenSolicitudResponse;
import co.edu.uniquindio.triage_academico.dto.response.SugerenciaClasificacionResponse;
import co.edu.uniquindio.triage_academico.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.triage_academico.repository.SolicitudRepository;
import co.edu.uniquindio.triage_academico.repository.SugerenciaIARepository;
import co.edu.uniquindio.triage_academico.service.impl.IAServiceImpl;

@ExtendWith(MockitoExtension.class)
class IAServiceImplTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private SugerenciaIARepository sugerenciaIARepository;  // ← Agregar

    @InjectMocks
    private IAServiceImpl iaService;

    private SolicitudAcademica solicitud;

    @BeforeEach
    void setUp() {
        solicitud = SolicitudAcademica.builder()
                .id(1L)
                .descripcion("Necesito homologar materias para graduarme")
                .estado(EstadoSolicitud.EN_ATENCION)
                .nivelPrioridad(NivelPrioridad.ALTA)
                .tipoSolicitud(TipoSolicitud.HOMOLOGACION)
                .solicitanteId(1L)
                .fechaCreacion(LocalDateTime.now())
                .historial(new ArrayList<>())
                .build();
    }

    // ==================== RF-10: Sugerir Clasificacion ====================

    @Test
    @DisplayName("Sugerir clasificacion con homologacion - fallback local")
    void sugerirClasificacion_conHomologacion_fallbackLocal() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        
        SugerenciaClasificacionResponse response = iaService.sugerirClasificacion(1L, "Necesito homologar mis materias");

        assertNotNull(response);
        assertEquals(TipoSolicitud.HOMOLOGACION, response.getTipoSugerido());
        assertEquals(NivelPrioridad.ALTA, response.getPrioridadSugerida());
        assertTrue(response.getExplicacion().contains("Sugerencia generada localmente"));
        assertEquals(0.5f, response.getConfianza());
        assertTrue(response.isRequiereConfirmacion());
    }

    @Test
    @DisplayName("Sugerir clasificacion con cancelacion - fallback local")
    void sugerirClasificacion_conCancelacion_fallbackLocal() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        
        SugerenciaClasificacionResponse response = iaService.sugerirClasificacion(1L, "Necesito cancelar una materia");

        assertNotNull(response);
        assertEquals(TipoSolicitud.CANCELACION, response.getTipoSugerido());
        assertEquals(NivelPrioridad.MEDIA, response.getPrioridadSugerida());
    }

    @Test
    @DisplayName("Sugerir clasificacion con cupo - fallback local")
    void sugerirClasificacion_conCupo_fallbackLocal() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        
        SugerenciaClasificacionResponse response = iaService.sugerirClasificacion(1L, "Solicito cupo para una materia");

        assertNotNull(response);
        assertEquals(TipoSolicitud.CUPOS, response.getTipoSugerido());
        assertEquals(NivelPrioridad.ALTA, response.getPrioridadSugerida());
    }

    @Test
    @DisplayName("Sugerir clasificacion con registro - fallback local")
    void sugerirClasificacion_conRegistro_fallbackLocal() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        
        SugerenciaClasificacionResponse response = iaService
                .sugerirClasificacion(1L, "Quiero registrar materias para el proximo semestre");

        assertNotNull(response);
        assertEquals(TipoSolicitud.REGISTRO_ASIGNATURAS, response.getTipoSugerido());
        assertEquals(NivelPrioridad.MEDIA, response.getPrioridadSugerida());
    }

    @Test
    @DisplayName("Sugerir clasificacion con consulta - fallback local")
    void sugerirClasificacion_conConsulta_fallbackLocal() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        
        SugerenciaClasificacionResponse response = iaService
                .sugerirClasificacion(1L, "Tengo una consulta sobre mi horario");

        assertNotNull(response);
        assertEquals(TipoSolicitud.CONSULTA, response.getTipoSugerido());
        assertEquals(NivelPrioridad.BAJA, response.getPrioridadSugerida());
    }

    @Test
    @DisplayName("Sugerir clasificacion con texto desconocido - fallback local")
    void sugerirClasificacion_conTextoDesconocido_fallbackLocal() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        
        SugerenciaClasificacionResponse response = iaService
                .sugerirClasificacion(1L, "Texto sin palabras clave especificas");

        assertNotNull(response);
        assertEquals(TipoSolicitud.OTRO, response.getTipoSugerido());
        assertEquals(NivelPrioridad.BAJA, response.getPrioridadSugerida());
    }

    @Test
    @DisplayName("RF-10: Sugerir clasificacion con descripcion nula - fallback local")
    void sugerirClasificacion_conDescripcionNula_fallbackLocal() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        
        SugerenciaClasificacionResponse response = iaService.sugerirClasificacion(1L, null);

        assertNotNull(response);
        assertEquals(TipoSolicitud.OTRO, response.getTipoSugerido());
        assertEquals(NivelPrioridad.BAJA, response.getPrioridadSugerida());
        assertTrue(response.getExplicacion().contains("No se pudo analizar"));
    }

    @Test
    @DisplayName("Sugerir clasificacion - solicitud no existe debe lanzar excepcion")
    void sugerirClasificacion_solicitudNoExiste_debeLanzarExcepcion() {
        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> iaService.sugerirClasificacion(99L, "Descripcion de prueba"));
    }

    // ==================== RF-09: Generar Resumen ====================

    @Test
    @DisplayName("Generar resumen - fallback manual")
    void generarResumen_fallbackManual() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));

        ResumenSolicitudResponse response = iaService.generarResumen(1L);

        assertNotNull(response);
        assertEquals(1L, response.getSolicitudId());
        assertTrue(response.getResumen().contains("Solicitud #1"));
        assertTrue(response.getResumen().contains("EN_ATENCION"));
        assertTrue(response.getResumen().contains("HOMOLOGACION"));
        assertTrue(response.getResumen().contains("ALTA"));
    }

    @Test
    @DisplayName("Generar resumen - solicitud no encontrada debe lanzar excepcion")
    void generarResumen_solicitudNoEncontrada_deberiaLanzarExcepcion() {
        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> iaService.generarResumen(99L));
    }

    @Test
    @DisplayName("RF-09: Generar resumen con historial vacio - fallback manual")
    void generarResumen_conHistorialVacio_fallbackManual() {
        solicitud.setHistorial(new ArrayList<>());
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));

        ResumenSolicitudResponse response = iaService.generarResumen(1L);

        assertNotNull(response);
        assertEquals(1L, response.getSolicitudId());
        assertNotNull(response.getResumen());
    }
}