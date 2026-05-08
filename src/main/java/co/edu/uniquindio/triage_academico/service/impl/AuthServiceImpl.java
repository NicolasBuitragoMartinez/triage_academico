package co.edu.uniquindio.triage_academico.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.triage_academico.Util.JwtUtils;
import co.edu.uniquindio.triage_academico.domain.Rol;
import co.edu.uniquindio.triage_academico.domain.Usuario;
import co.edu.uniquindio.triage_academico.dto.request.LoginRequest;
import co.edu.uniquindio.triage_academico.dto.request.RegistroUsuarioRequest;
import co.edu.uniquindio.triage_academico.dto.response.TokenResponse;
import co.edu.uniquindio.triage_academico.exception.BusinessException;
import co.edu.uniquindio.triage_academico.exception.UsuarioNoEncontradoException;
import co.edu.uniquindio.triage_academico.repository.RolRepository;
import co.edu.uniquindio.triage_academico.repository.UsuarioRepository;
import co.edu.uniquindio.triage_academico.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        log.info("Buscando usuario con email: {}", request.getEmail());

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));

        log.info("Usuario encontrado: {}", usuario.getEmail());
        log.info("Password BD: {}", usuario.getPassword());
        log.info("Password request: {}", request.getPassword());
        log.info("Matches: {}", passwordEncoder.matches(request.getPassword(), usuario.getPassword()));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales invalidas");
        }

        if (!usuario.isActivo()) {
            throw new BusinessException("Usuario inactivo");
        }

        String token = jwtUtils.generarToken(
                usuario.getEmail(),
                usuario.getRol().getNombre().name());

        return TokenResponse.builder()
                .token(token)
                .tipo("Bearer")
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .rol(usuario.getRol().getNombre().name())
                .build();
    }

    @Override
    @Transactional
    public TokenResponse registrar(RegistroUsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("El email ya esta registrado");
        }

        Rol rol = rolRepository.findByNombre(request.getRol())
                .orElseThrow(() -> new BusinessException("Rol no encontrado: " + request.getRol()));

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(rol)
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .build();

        usuarioRepository.save(usuario);

        String token = jwtUtils.generarToken(
                usuario.getEmail(),
                rol.getNombre().name());

        return TokenResponse.builder()
                .token(token)
                .tipo("Bearer")
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .rol(rol.getNombre().name())
                .build();
    }

    @Override
    public Usuario getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UsuarioNoEncontradoException("No hay usuario autenticado");
        }

        String email = auth.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado: " + email));
    }
}