package co.edu.uniquindio.triage_academico.service;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;

class PrioridadReglasServiceTest {

    private PrioridadReglasService service;

    @BeforeEach
    void setUp() {
        service = new PrioridadReglasService();
    }

    // Pruebas de prioridad por tipo

    @Test
    @DisplayName("Homologacion siempre prioridad ALTA")
    void testHomologacionPrioridadAlta() {
        NivelPrioridad prioridad = service.calcularPrioridad(TipoSolicitud.HOMOLOGACION, null);
        assertEquals(NivelPrioridad.ALTA, prioridad);
    }

    @Test
    @DisplayName("Cancelacion siempre prioridad ALTA")
    void testCancelacionPrioridadAlta() {
        NivelPrioridad prioridad = service.calcularPrioridad(TipoSolicitud.CANCELACION, null);
        assertEquals(NivelPrioridad.ALTA, prioridad);
    }

    @Test
    @DisplayName("Cupos siempre prioridad ALTA")
    void testCuposPrioridadAlta() {
        NivelPrioridad prioridad = service.calcularPrioridad(TipoSolicitud.CUPOS, null);
        assertEquals(NivelPrioridad.ALTA, prioridad);
    }

    @ParameterizedTest
    @CsvSource({
        "REGISTRO_ASIGNATURAS, MEDIA",
        "CONSULTA, BAJA",
        "OTRO, BAJA"
    })
    @DisplayName("Prioridades por defecto segun tipo")
    void testPrioridadesPorDefecto(TipoSolicitud tipo, NivelPrioridad esperada) {
        NivelPrioridad prioridad = service.calcularPrioridad(tipo, null);
        assertEquals(esperada, prioridad);
    }

    // Pruebas de prioridad por fecha limite 

    @Test
    @DisplayName("Fecha limite a menos de 3 dias -> CRITICA")
    void testFechaLimiteCritica() {
        LocalDateTime fechaLimite = LocalDateTime.now().plusDays(2);
        NivelPrioridad prioridad = service.calcularPrioridad(TipoSolicitud.REGISTRO_ASIGNATURAS, fechaLimite);
        assertEquals(NivelPrioridad.CRITICA, prioridad);
    }

    @Test
    @DisplayName("Fecha limite a menos de 3 dias (con tipo que seria BAJA) -> CRITICA (prioridad mas alta gana)")
    void testFechaLimiteCriticaSobreTipo() {
        LocalDateTime fechaLimite = LocalDateTime.now().plusDays(2);
        NivelPrioridad prioridad = service.calcularPrioridad(TipoSolicitud.CONSULTA, fechaLimite);
        assertEquals(NivelPrioridad.CRITICA, prioridad);
    }

    @Test
    @DisplayName("Fecha limite entre 4 y 7 dias -> ALTA")
    void testFechaLimiteAlta() {
        LocalDateTime fechaLimite = LocalDateTime.now().plusDays(5);
        NivelPrioridad prioridad = service.calcularPrioridad(TipoSolicitud.REGISTRO_ASIGNATURAS, fechaLimite);
        assertEquals(NivelPrioridad.ALTA, prioridad);
    }

    @Test
    @DisplayName("Fecha limite mayor a 7 dias -> prioridad por tipo")
    void testFechaLimiteMayor7Dias() {
        LocalDateTime fechaLimite = LocalDateTime.now().plusDays(10);
        NivelPrioridad prioridad = service.calcularPrioridad(TipoSolicitud.CONSULTA, fechaLimite);
        assertEquals(NivelPrioridad.BAJA, prioridad);
    }

    // Pruebas de justificacion

    @Test
    @DisplayName("Justificacion generada correctamente para homologacion")
    void testJustificacionHomologacion() {
        TipoSolicitud tipo = TipoSolicitud.HOMOLOGACION;
        NivelPrioridad prioridad = NivelPrioridad.ALTA;
        LocalDateTime fechaLimite = null;
        
        String justificacion = service.generarJustificacion(tipo, prioridad, fechaLimite);
        
        assertTrue(justificacion.contains("Prioridad ALTA asignada"));
        assertTrue(justificacion.contains("homologaciones requieren atencion prioritaria"));
    }

    @Test
    @DisplayName("Justificacion generada correctamente para consulta")
    void testJustificacionConsulta() {
        TipoSolicitud tipo = TipoSolicitud.CONSULTA;
        NivelPrioridad prioridad = NivelPrioridad.BAJA;
        LocalDateTime fechaLimite = null;
        
        String justificacion = service.generarJustificacion(tipo, prioridad, fechaLimite);
        
        assertTrue(justificacion.contains("Prioridad BAJA asignada"));
        assertTrue(justificacion.contains("Consulta academica de baja prioridad"));
    }

    @Test
    @DisplayName("Justificacion incluye fecha limite cuando existe")
    void testJustificacionConFechaLimite() {
        LocalDateTime fechaLimite = LocalDateTime.now().plusDays(2);
        String justificacion = service.generarJustificacion(
                TipoSolicitud.REGISTRO_ASIGNATURAS,
                NivelPrioridad.CRITICA,
                fechaLimite);
        
        assertTrue(justificacion.contains("Fecha limite a menos de 3 dias"));
    }

    @Test
    @DisplayName("Justificacion para fecha limite entre 4 y 7 dias")
    void testJustificacionConFechaLimiteAlta() {
        LocalDateTime fechaLimite = LocalDateTime.now().plusDays(5);
        String justificacion = service.generarJustificacion(
                TipoSolicitud.REGISTRO_ASIGNATURAS,
                NivelPrioridad.ALTA,
                fechaLimite);
        
        assertTrue(justificacion.contains("Fecha limite a menos de 7 dias"));
    }
}