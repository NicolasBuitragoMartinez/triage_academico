package co.edu.uniquindio.triage_academico.dto.request;

import co.edu.uniquindio.triage_academico.domain.enums.CanalOrigen;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EditarSolicitudRequest {
    @NotBlank(message = "La descripcion es obligatoria")
    private String descripcion;
    
    @NotNull(message = "El tipo de solicitud es obligatorio")
    private TipoSolicitud tipoSolicitud;

    @NotNull(message = "El canal de origen es obligatorio")
    private CanalOrigen canalOrigen;

    private Integer version;
}