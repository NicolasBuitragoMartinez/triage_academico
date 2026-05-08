package co.edu.uniquindio.triage_academico.controller;

import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import co.edu.uniquindio.triage_academico.dto.request.SugerenciaClasificacionRequest;
import co.edu.uniquindio.triage_academico.dto.response.ResumenSolicitudResponse;
import co.edu.uniquindio.triage_academico.dto.response.SugerenciaClasificacionResponse;
import co.edu.uniquindio.triage_academico.exception.IAException;
import co.edu.uniquindio.triage_academico.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.triage_academico.service.IAService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IAController.class)
class IAControllerTest2 {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private IAService iaService;

        @Test
        @DisplayName("POST /api/ia/sugerir-clasificacion - debe retornar 200")
        @WithMockUser(roles = "ADMINISTRATIVO")
        void sugerirClasificacion_debeRetornar200() throws Exception {
                SugerenciaClasificacionRequest request = new SugerenciaClasificacionRequest();
                request.setSolicitudId(1L);
                request.setDescripcion("Necesito homologar materias");

                SugerenciaClasificacionResponse response = SugerenciaClasificacionResponse.builder()
                                .tipoSugerido(TipoSolicitud.HOMOLOGACION)
                                .prioridadSugerida(NivelPrioridad.ALTA)
                                .explicacion("Explicacion de la IA")
                                .confianza(0.95f)
                                .requiereConfirmacion(true)
                                .fechaSugerencia(LocalDateTime.now())
                                .build();

                when(iaService.sugerirClasificacion(eq(1L), any(String.class))).thenReturn(response);

                mockMvc.perform(post("/api/ia/sugerir-clasificacion")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.tipoSugerido").value("HOMOLOGACION"))
                                .andExpect(jsonPath("$.prioridadSugerida").value("ALTA"))
                                .andExpect(jsonPath("$.confianza").value(0.95));
        }

        @Test
        @DisplayName("GET /api/ia/resumen/{solicitudId} - debe retornar 200")
        @WithMockUser(roles = "ADMINISTRATIVO")
        void generarResumen_debeRetornar200() throws Exception {
                ResumenSolicitudResponse response = ResumenSolicitudResponse.builder()
                                .solicitudId(1L)
                                .resumen("Resumen de la solicitud")
                                .build();

                when(iaService.generarResumen(eq(1L))).thenReturn(response);

                mockMvc.perform(get("/api/ia/resumen/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.solicitudId").value(1L))
                                .andExpect(jsonPath("$.resumen").value("Resumen de la solicitud"));
        }

        @Test
        @DisplayName("GET /api/ia/resumen/{solicitudId} - solicitud no existe debe retornar 404")
        @WithMockUser(roles = "ADMINISTRATIVO")
        void generarResumen_solicitudNoExiste_debeRetornar404() throws Exception {
                when(iaService.generarResumen(eq(999L)))
                                .thenThrow(new RecursoNoEncontradoException("Solicitud", 999L));

                mockMvc.perform(get("/api/ia/resumen/999"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/ia/resumen/{solicitudId} - IA no disponible debe retornar 503")
        @WithMockUser(roles = "ADMINISTRATIVO")
        void generarResumen_iaNoDisponible_debeRetornar503() throws Exception {
                when(iaService.generarResumen(eq(1L)))
                                .thenThrow(new IAException("Servicio de IA no disponible"));

                mockMvc.perform(get("/api/ia/resumen/1"))
                                .andExpect(status().isServiceUnavailable());
        }
}