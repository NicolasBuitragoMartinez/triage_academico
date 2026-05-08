package co.edu.uniquindio.triage_academico.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenSolicitudResponse {
    private Long solicitudId;
    private String resumen;
}