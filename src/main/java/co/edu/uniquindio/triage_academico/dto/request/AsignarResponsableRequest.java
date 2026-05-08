package co.edu.uniquindio.triage_academico.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignarResponsableRequest {
    @NotNull(message = "El ID del responsable es obligatorio")
    private Long responsableId;

    private Integer version;
}
