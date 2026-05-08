package co.edu.uniquindio.triage_academico.service;

import co.edu.uniquindio.triage_academico.domain.Usuario;
import co.edu.uniquindio.triage_academico.dto.request.LoginRequest;
import co.edu.uniquindio.triage_academico.dto.request.RegistroUsuarioRequest;
import co.edu.uniquindio.triage_academico.dto.response.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);

    TokenResponse registrar(RegistroUsuarioRequest request);

    Usuario getUsuarioAutenticado();
}