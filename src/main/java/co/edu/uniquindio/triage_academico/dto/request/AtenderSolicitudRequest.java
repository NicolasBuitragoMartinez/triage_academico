package co.edu.uniquindio.triage_academico.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtenderSolicitudRequest {
    @NotBlank(message = "La observacion de resolucion es obligatoria")
    private String observacion;

    private Integer version;
}
