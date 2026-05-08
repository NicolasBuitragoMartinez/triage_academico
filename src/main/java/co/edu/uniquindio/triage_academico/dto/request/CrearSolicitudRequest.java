package co.edu.uniquindio.triage_academico.dto.request;

import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import co.edu.uniquindio.triage_academico.domain.enums.CanalOrigen;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearSolicitudRequest {
    @NotBlank(message = "La descripcion de la solicitud es obligatoria")
    private String descripcion;

    @NotNull(message = "El tipo de solicitud es obligatorio")
    private TipoSolicitud tipoSolicitud;

    @NotNull(message = "El ID del solicitante es obligatorio")
    private Long solicitanteId;

    @NotNull(message = "El canal de origen es obligatorio")
    private CanalOrigen canalOrigen;

    private Integer version;
}