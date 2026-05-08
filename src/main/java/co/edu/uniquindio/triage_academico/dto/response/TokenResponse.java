package co.edu.uniquindio.triage_academico.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String token;
    private String tipo;
    private String email;
    private String nombre;
    private String rol;
}