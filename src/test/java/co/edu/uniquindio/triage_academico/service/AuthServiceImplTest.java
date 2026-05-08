package co.edu.uniquindio.triage_academico.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.uniquindio.triage_academico.Util.JwtUtils;
import co.edu.uniquindio.triage_academico.domain.Rol;
import co.edu.uniquindio.triage_academico.domain.Usuario;
import co.edu.uniquindio.triage_academico.domain.enums.NombreRol;
import co.edu.uniquindio.triage_academico.dto.request.LoginRequest;
import co.edu.uniquindio.triage_academico.dto.request.RegistroUsuarioRequest;
import co.edu.uniquindio.triage_academico.dto.response.TokenResponse;
import co.edu.uniquindio.triage_academico.exception.BusinessException;
import co.edu.uniquindio.triage_academico.exception.UsuarioNoEncontradoException;
import co.edu.uniquindio.triage_academico.repository.RolRepository;
import co.edu.uniquindio.triage_academico.repository.UsuarioRepository;
import co.edu.uniquindio.triage_academico.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario usuario;
    private Rol rol;
    private LoginRequest loginRequest;
    private RegistroUsuarioRequest registroRequest;

    @BeforeEach
    void setUp() {
        rol = Rol.builder()
                .id(1L)
                .nombre(NombreRol.ESTUDIANTE)
                .build();

        usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan")
                .apellido("Perez")
                .email("juan@uniquindio.edu.co")
                .password("encodedPassword123")
                .rol(rol)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setEmail("juan@uniquindio.edu.co");
        loginRequest.setPassword("1234");

        registroRequest = new RegistroUsuarioRequest();
        registroRequest.setNombre("Ana");
        registroRequest.setApellido("Garcia");
        registroRequest.setEmail("ana@uniquindio.edu.co");
        registroRequest.setPassword("1234");
        registroRequest.setRol(NombreRol.ESTUDIANTE);
    }

    @Test
    @DisplayName("Login exitoso debe retornar token")
    void login_deberiaRetornarToken() {
        when(usuarioRepository.findByEmail("juan@uniquindio.edu.co")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "encodedPassword123")).thenReturn(true);
        when(jwtUtils.generarToken(usuario.getEmail(), usuario.getRol().getNombre().name()))
                .thenReturn("jwt-token-123");

        TokenResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token-123", response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals("juan@uniquindio.edu.co", response.getEmail());
        assertEquals("Juan", response.getNombre());
        assertEquals("ESTUDIANTE", response.getRol());
    }

    @Test
    @DisplayName("Login con email inexistente debe lanzar BadCredentialsException")
    void login_emailNoExiste_deberiaLanzarExcepcion() {
        when(usuarioRepository.findByEmail("juan@uniquindio.edu.co")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login con contraseña incorrecta debe lanzar BadCredentialsException")
    void login_passwordIncorrecta_deberiaLanzarExcepcion() {
        when(usuarioRepository.findByEmail("juan@uniquindio.edu.co")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "encodedPassword123")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login con usuario inactivo debe lanzar BusinessException")
    void login_usuarioInactivo_deberiaLanzarExcepcion() {
        usuario.setActivo(false);
        when(usuarioRepository.findByEmail("juan@uniquindio.edu.co")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "encodedPassword123")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.login(loginRequest));
    }

    // ==================== Registro ====================

    @Test
    @DisplayName("Registro exitoso debe retornar token")
    void registrar_deberiaRetornarToken() {
        when(usuarioRepository.existsByEmail("ana@uniquindio.edu.co")).thenReturn(false);
        when(rolRepository.findByNombre(NombreRol.ESTUDIANTE)).thenReturn(Optional.of(rol));
        when(passwordEncoder.encode("1234")).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(jwtUtils.generarToken("ana@uniquindio.edu.co", "ESTUDIANTE")).thenReturn("jwt-token-456");

        TokenResponse response = authService.registrar(registroRequest);

        assertNotNull(response);
        assertEquals("jwt-token-456", response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals("ana@uniquindio.edu.co", response.getEmail());
        assertEquals("Ana", response.getNombre());
        assertEquals("ESTUDIANTE", response.getRol());

        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Registro con email ya existente debe lanzar BusinessException")
    void registrar_emailYaExiste_deberiaLanzarExcepcion() {
        when(usuarioRepository.existsByEmail("ana@uniquindio.edu.co")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.registrar(registroRequest));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registro con rol no existente debe lanzar BusinessException")
    void registrar_rolNoExiste_deberiaLanzarExcepcion() {
        when(usuarioRepository.existsByEmail("ana@uniquindio.edu.co")).thenReturn(false);
        when(rolRepository.findByNombre(NombreRol.ESTUDIANTE)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.registrar(registroRequest));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUsuarioAutenticado con usuario logueado debe retornar usuario")
    void getUsuarioAutenticado_conUsuarioLogueado_deberiaRetornarUsuario() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("juan@uniquindio.edu.co");
        when(usuarioRepository.findByEmail("juan@uniquindio.edu.co")).thenReturn(Optional.of(usuario));

        Usuario result = authService.getUsuarioAutenticado();

        assertNotNull(result);
        assertEquals("juan@uniquindio.edu.co", result.getEmail());
    }

    @Test
    @DisplayName("getUsuarioAutenticado sin autenticacion debe lanzar excepcion")
    void getUsuarioAutenticado_sinAutenticacion_deberiaLanzarExcepcion() {
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        assertThrows(UsuarioNoEncontradoException.class, () -> authService.getUsuarioAutenticado());
    }
}