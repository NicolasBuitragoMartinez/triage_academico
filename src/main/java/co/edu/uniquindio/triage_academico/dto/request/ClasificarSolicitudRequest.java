package co.edu.uniquindio.triage_academico.dto.request;

import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClasificarSolicitudRequest {
        @NotNull(message = "El tipo de solicitud es obligatorio")
        private TipoSolicitud tipoSolicitud;

        //RF-03
        @NotNull(message = "La fecha limite es obligatoria")
        private LocalDateTime fechaLimite;

        private Integer version;
}       