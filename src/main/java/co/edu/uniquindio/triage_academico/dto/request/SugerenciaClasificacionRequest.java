package co.edu.uniquindio.triage_academico.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SugerenciaClasificacionRequest {
    @NotNull(message = "El ID de la solicitud es obligatorio")
    private Long solicitudId;

    @NotBlank(message = "La descripcion es obligatoria")
    private String descripcion;
}