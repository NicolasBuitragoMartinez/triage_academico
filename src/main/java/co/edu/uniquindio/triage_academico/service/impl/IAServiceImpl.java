package co.edu.uniquindio.triage_academico.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.uniquindio.triage_academico.domain.SolicitudAcademica;
import co.edu.uniquindio.triage_academico.domain.SugerenciaIA;
import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import co.edu.uniquindio.triage_academico.dto.response.ResumenSolicitudResponse;
import co.edu.uniquindio.triage_academico.dto.response.SugerenciaClasificacionResponse;
import co.edu.uniquindio.triage_academico.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.triage_academico.repository.SolicitudRepository;
import co.edu.uniquindio.triage_academico.repository.SugerenciaIARepository;
import co.edu.uniquindio.triage_academico.service.IAService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IAServiceImpl implements IAService {

    private final SolicitudRepository solicitudRepository;
    private final SugerenciaIARepository sugerenciaIARepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ia.habilitada:false}")
    private boolean iaHabilitada;

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    // RF-10: Sugerir clasificacion
    @Override
    @Transactional
    public SugerenciaClasificacionResponse sugerirClasificacion(Long solicitudId, String descripcion) {
        SolicitudAcademica solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud", solicitudId));

        SugerenciaClasificacionResponse response;

        if (!iaHabilitada || apiKey == null || apiKey.isEmpty()) {
            log.info("IA no habilitada o sin API key, usando fallback local");
            response = sugerenciaDefault(descripcion);
        } else {
            String prompt = buildPromptClasificacion(descripcion);
            try {
                String respuestaIA = llamarGroqAPI(prompt);
                JsonNode json = objectMapper.readTree(respuestaIA);

                response = SugerenciaClasificacionResponse.builder()
                        .tipoSugerido(TipoSolicitud.valueOf(json.get("tipoSugerido").asText()))
                        .prioridadSugerida(NivelPrioridad.valueOf(json.get("prioridadSugerida").asText()))
                        .explicacion(json.get("explicacion").asText())
                        .confianza(json.get("confianza").floatValue())
                        .requiereConfirmacion(true)
                        .fechaSugerencia(LocalDateTime.now())
                        .build();

            } catch (Exception e) {
                log.error("Error al llamar a Groq API, usando fallback local", e);
                response = sugerenciaDefault(descripcion);
            }
        }

        guardarSugerencia(solicitud, response);

        return response;
    }

    // RF-09: Generar resumen
    
    @Override
    @Transactional
    public ResumenSolicitudResponse generarResumen(Long solicitudId) {
        if (!iaHabilitada || apiKey == null || apiKey.isEmpty()) {
            log.info("IA no habilitada o sin API key, usando resumen manual");
            return generarResumenManual(solicitudId);
        }

        SolicitudAcademica solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud", solicitudId));

        String prompt = buildPromptResumen(solicitud);

        try {
            String resumen = llamarGroqAPI(prompt);
            return ResumenSolicitudResponse.builder()
                    .solicitudId(solicitudId)
                    .resumen(resumen)
                    .build();

        } catch (Exception e) {
            log.error("Error al llamar a Groq API, usando resumen manual", e);
            return generarResumenManual(solicitudId);
        }
    }

    private void guardarSugerencia(SolicitudAcademica solicitud, SugerenciaClasificacionResponse response) {
        SugerenciaIA sugerencia = SugerenciaIA.builder()
                .solicitud(solicitud)
                .tipoSugerido(response.getTipoSugerido())
                .prioridadSugerida(response.getPrioridadSugerida())
                .explicacion(response.getExplicacion())
                .confianza(response.getConfianza())
                .fechaSugerencia(response.getFechaSugerencia())
                .aplicada(false)
                .build();

        sugerenciaIARepository.save(sugerencia);
        log.info("Sugerencia IA guardada para solicitud ID: {}", solicitud.getId());
    }

    // Metodos de fallback local

    private SugerenciaClasificacionResponse sugerenciaDefault(String descripcion) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return SugerenciaClasificacionResponse.builder()
                    .tipoSugerido(TipoSolicitud.OTRO)
                    .prioridadSugerida(NivelPrioridad.BAJA)
                    .explicacion("No se pudo analizar la descripcion. Sugerencia por defecto.")
                    .confianza(0.3f)
                    .requiereConfirmacion(true)
                    .fechaSugerencia(LocalDateTime.now())
                    .build();
        }

        String desc = descripcion.toLowerCase();
        TipoSolicitud tipo;
        NivelPrioridad prioridad;

        if (desc.contains("homolog")) {
            tipo = TipoSolicitud.HOMOLOGACION;
            prioridad = NivelPrioridad.ALTA;
        } else if (desc.contains("cancel")) {
            tipo = TipoSolicitud.CANCELACION;
            prioridad = NivelPrioridad.MEDIA;
        } else if (desc.contains("cupo")) {
            tipo = TipoSolicitud.CUPOS;
            prioridad = NivelPrioridad.ALTA;
        } else if (desc.contains("registrar") || desc.contains("inscribir")) {
            tipo = TipoSolicitud.REGISTRO_ASIGNATURAS;
            prioridad = NivelPrioridad.MEDIA;
        } else if (desc.contains("consulta")) {
            tipo = TipoSolicitud.CONSULTA;
            prioridad = NivelPrioridad.BAJA;
        } else {
            tipo = TipoSolicitud.OTRO;
            prioridad = NivelPrioridad.BAJA;
        }

        return SugerenciaClasificacionResponse.builder()
                .tipoSugerido(tipo)
                .prioridadSugerida(prioridad)
                .explicacion("Sugerencia generada localmente sin IA externa.")
                .confianza(0.5f)
                .requiereConfirmacion(true)
                .fechaSugerencia(LocalDateTime.now())
                .build();
    }

    private ResumenSolicitudResponse generarResumenManual(Long solicitudId) {
        SolicitudAcademica solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud", solicitudId));

        return ResumenSolicitudResponse.builder()
                .solicitudId(solicitudId)
                .resumen("Solicitud #" + solicitudId + " — Estado: " + solicitud.getEstado()
                        + " — Tipo: " + solicitud.getTipoSolicitud()
                        + " — Prioridad: " + solicitud.getNivelPrioridad())
                .build();
    }

    // Metodos de IA

    private String buildPromptClasificacion(String descripcion) {
        return """
                Eres un asistente de gestion academica universitaria.
                Analiza esta solicitud estudiantil y sugiere:
                1. El tipo de solicitud (uno de: REGISTRO_ASIGNATURAS, HOMOLOGACION, CANCELACION, CUPOS, CONSULTA, OTRO)
                2. El nivel de prioridad (uno de: BAJA, MEDIA, ALTA, CRITICA)
                3. Una explicacion breve de maximo 2 oraciones
                4. Un valor de confianza entre 0.0 y 1.0

                Solicitud: "%s"

                Responde UNICAMENTE en este formato JSON sin texto adicional:
                {
                    "tipoSugerido": "TIPO_AQUI",
                    "prioridadSugerida": "PRIORIDAD_AQUI",
                    "explicacion": "explicacion aqui",
                    "confianza": 0.0
                }
                """.formatted(descripcion);
    }

    private String buildPromptResumen(SolicitudAcademica solicitud) {
        String historialTexto = (solicitud.getHistorial() == null || solicitud.getHistorial().isEmpty())
                ? "Sin eventos registrados"
                : solicitud.getHistorial().stream()
                        .map(h -> "- [" + h.getFechaHoraAccion() + "] " + h.getAccion() + ": " + h.getObservacion())
                        .collect(Collectors.joining("\n"));

        return """
                Eres un asistente academico. Resume brevemente el estado actual de esta solicitud universitaria.
                El resumen debe ser claro, profesional y de maximo 3 oraciones.

                Solicitud ID: %d
                Descripción: %s
                Estado actual: %s
                Tipo: %s
                Prioridad: %s
                Historial:
                %s

                Responde UNICAMENTE con el texto del resumen, sin formato adicional.
                """.formatted(
                solicitud.getId(),
                solicitud.getDescripcion(),
                solicitud.getEstado(),
                solicitud.getTipoSolicitud(),
                solicitud.getNivelPrioridad(),
                historialTexto);
    }

    private String llamarGroqAPI(String prompt) throws Exception {
        String body = """
                {
                    "model": "%s",
                    "messages": [
                        {"role": "user", "content": "%s"}
                    ],
                    "temperature": 0.7,
                    "max_tokens": 500
                }
                """.formatted(model, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Error API Groq: Status {} - Body: {}", response.statusCode(), response.body());
            throw new RuntimeException("Error API Groq: " + response.statusCode() + " - " + response.body());
        }

        JsonNode jsonResponse = objectMapper.readTree(response.body());
        return jsonResponse
                .get("choices").get(0)
                .get("message")
                .get("content").asText().trim();
    }
}