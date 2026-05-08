package co.edu.uniquindio.triage_academico.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CerrarSolicitudRequest {
    @NotBlank(message = "La observacion de cierre es obligatoria")
    private String observacionCierre;
    
    private Integer version;
}