package co.edu.uniquindio.triage_academico.dto.response;

import java.time.LocalDateTime;

import co.edu.uniquindio.triage_academico.domain.enums.EstadoSolicitud;
import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudResponse {
    private Long id;
    private String descripcion;
    private EstadoSolicitud estado;
    private NivelPrioridad nivelPrioridad;
    private TipoSolicitud tipoSolicitud;
    private Long solicitanteId;
    private Long responsableId;
    private String responsableNombre;
    private String responsableApellido;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String justificacionPrioridad;
    private LocalDateTime fechaLimite;
    private LocalDateTime fechaCierre;
    private LocalDateTime fechaResolucion;
    private String observacionCierre;
    private List<HistorialSolicitudResponse> historial;
}
