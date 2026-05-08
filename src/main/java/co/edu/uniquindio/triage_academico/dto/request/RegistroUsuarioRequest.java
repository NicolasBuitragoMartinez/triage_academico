package co.edu.uniquindio.triage_academico.dto.request;

import co.edu.uniquindio.triage_academico.domain.enums.NombreRol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistroUsuarioRequest {
    @NotBlank
    private String nombre;
    @NotBlank
    private String apellido;
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
    @NotNull
    private NombreRol rol;
}