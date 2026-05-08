package co.edu.uniquindio.triage_academico.dto.response;

import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SugerenciaClasificacionResponse {
    private TipoSolicitud tipoSugerido;
    private NivelPrioridad prioridadSugerida;
    private String explicacion;
    private Float confianza;
    private boolean requiereConfirmacion = true;
    private LocalDateTime fechaSugerencia;
}