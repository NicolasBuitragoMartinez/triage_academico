package co.edu.uniquindio.triage_academico.dto.response;

import java.time.LocalDateTime;

import co.edu.uniquindio.triage_academico.domain.enums.AccionHistorial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialSolicitudResponse {
    private Long id;
    private LocalDateTime fechaHoraAccion;
    private AccionHistorial accion;
    private Long usuarioId;
    private String observacion;
}