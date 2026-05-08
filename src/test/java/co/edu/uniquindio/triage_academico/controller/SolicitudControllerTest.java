package co.edu.uniquindio.triage_academico.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.uniquindio.triage_academico.Util.JwtUtils;
import co.edu.uniquindio.triage_academico.domain.enums.CanalOrigen;
import co.edu.uniquindio.triage_academico.domain.enums.EstadoSolicitud;
import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import co.edu.uniquindio.triage_academico.dto.request.AsignarResponsableRequest;
import co.edu.uniquindio.triage_academico.dto.request.AtenderSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.CerrarSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.ClasificarSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.CrearSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.response.SolicitudResponse;
import co.edu.uniquindio.triage_academico.exception.InvalidTransitionException;
import co.edu.uniquindio.triage_academico.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.triage_academico.service.SolicitudService;

@WebMvcTest(SolicitudController.class)
class SolicitudControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private SolicitudService solicitudService;

        @MockBean
        private JwtUtils jwtUtils;

        private SolicitudResponse crearResponseMock() {
                return SolicitudResponse.builder()
                                .id(1L)
                                .descripcion("Solicitud de prueba para test de integración")
                                .estado(EstadoSolicitud.REGISTRADA)
                                .nivelPrioridad(NivelPrioridad.BAJA)
                                .tipoSolicitud(TipoSolicitud.HOMOLOGACION)
                                .solicitanteId(1L)
                                .fechaCreacion(LocalDateTime.now())
                                .fechaActualizacion(LocalDateTime.now())
                                .build();
        }

        private SolicitudResponse crearSolicitudClasificadaResponseMock() {
                return SolicitudResponse.builder()
                                .id(1L)
                                .descripcion("Solicitud de prueba")
                                .estado(EstadoSolicitud.CLASIFICADA)
                                .nivelPrioridad(NivelPrioridad.ALTA)
                                .tipoSolicitud(TipoSolicitud.HOMOLOGACION)
                                .solicitanteId(1L)
                                .justificacionPrioridad("Prioridad ALTA asignada por reglas de negocio")
                                .fechaLimite(LocalDateTime.now().plusDays(5))
                                .fechaCreacion(LocalDateTime.now())
                                .fechaActualizacion(LocalDateTime.now())
                                .build();
        }

        private SolicitudResponse crearSolicitudEnAtencionResponseMock() {
                return SolicitudResponse.builder()
                                .id(1L)
                                .descripcion("Solicitud de prueba")
                                .estado(EstadoSolicitud.EN_ATENCION)
                                .nivelPrioridad(NivelPrioridad.ALTA)
                                .tipoSolicitud(TipoSolicitud.HOMOLOGACION)
                                .solicitanteId(1L)
                                .responsableId(3L)
                                .responsableNombre("Maria")
                                .responsableApellido("Lopez")
                                .fechaCreacion(LocalDateTime.now())
                                .fechaActualizacion(LocalDateTime.now())
                                .build();
        }

        private SolicitudResponse crearSolicitudAtendidaResponseMock() {
                return SolicitudResponse.builder()
                                .id(1L)
                                .descripcion("Solicitud de prueba")
                                .estado(EstadoSolicitud.ATENDIDA)
                                .nivelPrioridad(NivelPrioridad.ALTA)
                                .tipoSolicitud(TipoSolicitud.HOMOLOGACION)
                                .solicitanteId(1L)
                                .responsableId(3L)
                                .responsableNombre("Maria")
                                .responsableApellido("Lopez")
                                .fechaResolucion(LocalDateTime.now())
                                .fechaCreacion(LocalDateTime.now())
                                .fechaActualizacion(LocalDateTime.now())
                                .build();
        }

        private SolicitudResponse crearSolicitudCerradaResponseMock() {
                return SolicitudResponse.builder()
                                .id(1L)
                                .descripcion("Solicitud de prueba")
                                .estado(EstadoSolicitud.CERRADA)
                                .nivelPrioridad(NivelPrioridad.ALTA)
                                .tipoSolicitud(TipoSolicitud.HOMOLOGACION)
                                .solicitanteId(1L)
                                .responsableId(3L)
                                .responsableNombre("Maria")
                                .responsableApellido("Lopez")
                                .observacionCierre("Proceso finalizado correctamente")
                                .fechaCierre(LocalDateTime.now())
                                .fechaCreacion(LocalDateTime.now())
                                .fechaActualizacion(LocalDateTime.now())
                                .build();
        }

        // RF-01: Crear Solicitud

        @Test
        @DisplayName("POST /api/solicitudes - debe retornar 201 al crear solicitud")
        @WithMockUser(roles = "ESTUDIANTE")
        void crearSolicitud_debeRetornar201() throws Exception {
                CrearSolicitudRequest request = CrearSolicitudRequest.builder()
                                .descripcion("Necesito homologar materias")
                                .tipoSolicitud(TipoSolicitud.HOMOLOGACION)
                                .canalOrigen(CanalOrigen.CORREO)
                                .solicitanteId(1L)
                                .build();

                SolicitudResponse response = crearResponseMock();

                when(solicitudService.crearSolicitud(any(CrearSolicitudRequest.class), anyLong()))
                                .thenReturn(response);

                mockMvc.perform(post("/api/solicitudes")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1L))
                                .andExpect(jsonPath("$.estado").value("REGISTRADA"));
        }

        @Test
        @DisplayName("POST /api/solicitudes - con datos invalidos debe retornar 400")
        @WithMockUser(roles = "ESTUDIANTE")
        void crearSolicitud_conDatosInvalidos_debeRetornar400() throws Exception {
                CrearSolicitudRequest request = CrearSolicitudRequest.builder()
                                .descripcion("") // inválido
                                .tipoSolicitud(TipoSolicitud.HOMOLOGACION)
                                .canalOrigen(CanalOrigen.CORREO)
                                .solicitanteId(1L)
                                .build();

                mockMvc.perform(post("/api/solicitudes")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // RF-02 + RF-03: Clasificar

        @Test
        @DisplayName("PATCH /api/solicitudes/{id}/clasificar - debe retornar 200")
        @WithMockUser(roles = "ADMINISTRATIVO")
        void clasificarSolicitud_debeRetornar200() throws Exception {
                ClasificarSolicitudRequest request = ClasificarSolicitudRequest.builder()
                                .tipoSolicitud(TipoSolicitud.HOMOLOGACION)
                                .fechaLimite(LocalDateTime.now().plusDays(5))
                                .build();

                SolicitudResponse response = crearSolicitudClasificadaResponseMock();

                when(solicitudService.clasificarSolicitud(any(ClasificarSolicitudRequest.class), eq(1L)))
                                .thenReturn(response);

                mockMvc.perform(patch("/api/solicitudes/1/clasificar")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.estado").value("CLASIFICADA"))
                                .andExpect(jsonPath("$.nivelPrioridad").value("ALTA"));
        }

        @Test
        @DisplayName("PATCH /api/solicitudes/{id}/clasificar - transicion invalida debe retornar 400")
        @WithMockUser(roles = "ADMINISTRATIVO")
        void clasificarSolicitud_transicionInvalida_debeRetornar400() throws Exception {
                ClasificarSolicitudRequest request = ClasificarSolicitudRequest.builder()
                                .tipoSolicitud(TipoSolicitud.HOMOLOGACION)
                                .fechaLimite(LocalDateTime.now().plusDays(5))
                                .build();

                when(solicitudService.clasificarSolicitud(any(), eq(1L)))
                                .thenThrow(new InvalidTransitionException("Estado invalido"));

                mockMvc.perform(patch("/api/solicitudes/1/clasificar")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // RF-05: Asignar Responsable

        @Test
        @DisplayName("PATCH /api/solicitudes/{id}/asignar - debe retornar 200")
        @WithMockUser(roles = "COORDINADOR")
        void asignarResponsable_debeRetornar200() throws Exception {
                AsignarResponsableRequest request = AsignarResponsableRequest.builder()
                                .responsableId(3L)
                                .build();

                SolicitudResponse response = crearSolicitudEnAtencionResponseMock();

                when(solicitudService.asignarResponsable(any(AsignarResponsableRequest.class), eq(1L)))
                                .thenReturn(response);

                mockMvc.perform(patch("/api/solicitudes/1/asignar")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.estado").value("EN_ATENCION"))
                                .andExpect(jsonPath("$.responsableId").value(3L));
        }

        @Test
        @DisplayName("PATCH /api/solicitudes/{id}/asignar - sin responsableId debe retornar 400")
        @WithMockUser(roles = "COORDINADOR")
        void asignarResponsable_sinResponsableId_debeRetornar400() throws Exception {
                AsignarResponsableRequest request = AsignarResponsableRequest.builder()
                                .responsableId(null)
                                .build();

                mockMvc.perform(patch("/api/solicitudes/1/asignar")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PATCH /api/solicitudes/{id}/asignar - solicitud no existe debe retornar 404")
        @WithMockUser(roles = "COORDINADOR")
        void asignarResponsable_solicitudNoExiste_debeRetornar404() throws Exception {
                AsignarResponsableRequest request = AsignarResponsableRequest.builder()
                                .responsableId(3L)
                                .build();

                when(solicitudService.asignarResponsable(any(), eq(999L)))
                                .thenThrow(new RecursoNoEncontradoException("Solicitud no encontrada"));

                mockMvc.perform(patch("/api/solicitudes/999/asignar")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        // RF-04: Atender Solicitud

        @Test
        @DisplayName("PATCH /api/solicitudes/{id}/atender - debe retornar 200")
        @WithMockUser(roles = "ADMINISTRATIVO")
        void atenderSolicitud_debeRetornar200() throws Exception {
                AtenderSolicitudRequest request = AtenderSolicitudRequest.builder()
                                .observacion("Se aprueba la homologacion")
                                .build();

                SolicitudResponse response = crearSolicitudAtendidaResponseMock();

                when(solicitudService.atenderSolicitud(any(AtenderSolicitudRequest.class), eq(1L)))
                                .thenReturn(response);

                mockMvc.perform(patch("/api/solicitudes/1/atender")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.estado").value("ATENDIDA"));
        }

        @Test
        @DisplayName("PATCH /api/solicitudes/{id}/atender - sin observacion debe retornar 400")
        @WithMockUser(roles = "ADMINISTRATIVO")
        void atenderSolicitud_sinObservacion_debeRetornar400() throws Exception {
                AtenderSolicitudRequest request = AtenderSolicitudRequest.builder()
                                .observacion("")
                                .build();

                mockMvc.perform(patch("/api/solicitudes/1/atender")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // RF-04 + RF-08: Cerrar Solicitud

        @Test
        @DisplayName("PATCH /api/solicitudes/{id}/cerrar - debe retornar 200")
        @WithMockUser(roles = "ADMINISTRATIVO")
        void cerrarSolicitud_debeRetornar200() throws Exception {
                CerrarSolicitudRequest request = CerrarSolicitudRequest.builder()
                                .observacionCierre("Proceso finalizado correctamente")
                                .build();

                SolicitudResponse response = crearSolicitudCerradaResponseMock();

                when(solicitudService.cerrarSolicitud(any(CerrarSolicitudRequest.class), eq(1L)))
                                .thenReturn(response);

                mockMvc.perform(patch("/api/solicitudes/1/cerrar")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.estado").value("CERRADA"))
                                .andExpect(jsonPath("$.observacionCierre").value("Proceso finalizado correctamente"));
        }

        @Test
        @DisplayName("PATCH /api/solicitudes/{id}/cerrar - sin observacion debe retornar 400")
        @WithMockUser(roles = "ADMINISTRATIVO")
        void cerrarSolicitud_sinObservacion_debeRetornar400() throws Exception {
                CerrarSolicitudRequest request = CerrarSolicitudRequest.builder()
                                .observacionCierre("")
                                .build();

                mockMvc.perform(patch("/api/solicitudes/1/cerrar")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }
}