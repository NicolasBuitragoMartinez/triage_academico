package co.edu.uniquindio.triage_academico.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import co.edu.uniquindio.triage_academico.domain.Asignacion;
import co.edu.uniquindio.triage_academico.domain.Rol;
import co.edu.uniquindio.triage_academico.domain.SolicitudAcademica;
import co.edu.uniquindio.triage_academico.domain.Usuario;
import co.edu.uniquindio.triage_academico.domain.enums.CanalOrigen;
import co.edu.uniquindio.triage_academico.domain.enums.EstadoSolicitud;
import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.NombreRol;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import co.edu.uniquindio.triage_academico.dto.request.AsignarResponsableRequest;
import co.edu.uniquindio.triage_academico.dto.request.AtenderSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.CerrarSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.ClasificarSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.CrearSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.response.SolicitudResponse;
import co.edu.uniquindio.triage_academico.exception.InvalidTransitionException;
import co.edu.uniquindio.triage_academico.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.triage_academico.repository.AsignacionRepository;
import co.edu.uniquindio.triage_academico.repository.SolicitudRepository;
import co.edu.uniquindio.triage_academico.repository.UsuarioRepository;
import co.edu.uniquindio.triage_academico.service.impl.SolicitudServiceImpl;

@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplTest {

        @Mock
        private SolicitudRepository solicitudRepository;

        @Mock
        private UsuarioRepository usuarioRepository;

        @Mock
        private AsignacionRepository asignacionRepository;

        @Mock
        private PrioridadReglasService prioridadReglasService;

        @Mock
        private AuthService authService;

        @InjectMocks
        private SolicitudServiceImpl solicitudService;

        private SolicitudAcademica solicitudBase;
        private Usuario responsable;
        private Usuario ejecutor;

        @BeforeEach
        void setUp() {
                Rol rol = Rol.builder()
                                .id(1L)
                                .nombre(NombreRol.ADMINISTRATIVO)
                                .build();

                responsable = Usuario.builder()
                                .id(3L)
                                .nombre("Maria")
                                .apellido("Lopez")
                                .email("maria@uniquindio.edu.co")
                                .password("1234")
                                .rol(rol)
                                .activo(true)
                                .build();

                ejecutor = Usuario.builder()
                                .id(1L)
                                .nombre("Juan")
                                .apellido("Perez")
                                .email("juan@uniquindio.edu.co")
                                .password("1234")
                                .rol(rol)
                                .activo(true)
                                .build();

                solicitudBase = SolicitudAcademica.builder()
                                .id(1L)
                                .descripcion("Necesito homologar materias")
                                .estado(EstadoSolicitud.REGISTRADA)
                                .nivelPrioridad(NivelPrioridad.BAJA)
                                .tipoSolicitud(TipoSolicitud.HOMOLOGACION)
                                .canalOrigen(CanalOrigen.CORREO)
                                .solicitanteId(1L)
                                .fechaCreacion(LocalDateTime.now())
                                .asignaciones(new HashSet<>())
                                .historial(new ArrayList<>())
                                .version(0)
                                .build();
        }

        // RF-01
        @Test
        void crearSolicitud_deberiaRetornarSolicitudEnEstadoRegistrada() {
                CrearSolicitudRequest request = new CrearSolicitudRequest();
                request.setDescripcion("Necesito homologar materias");
                request.setTipoSolicitud(TipoSolicitud.HOMOLOGACION);
                request.setCanalOrigen(CanalOrigen.CORREO);
                request.setSolicitanteId(1L);

                when(authService.getUsuarioAutenticado()).thenReturn(ejecutor);
                when(solicitudRepository.save(any(SolicitudAcademica.class))).thenReturn(solicitudBase);

                SolicitudResponse response = solicitudService.crearSolicitud(request, 1L);

                assertNotNull(response);
                assertEquals(EstadoSolicitud.REGISTRADA, response.getEstado());
                assertEquals("Necesito homologar materias", response.getDescripcion());
                verify(solicitudRepository, atLeastOnce()).save(any());
        }

        // RF-02 + RF-03
        @Test
        void clasificarSolicitud_deberiaClasificarCorrectamente() {
                ClasificarSolicitudRequest request = new ClasificarSolicitudRequest();
                request.setTipoSolicitud(TipoSolicitud.HOMOLOGACION);
                request.setFechaLimite(LocalDateTime.now().plusDays(10));
                request.setVersion(0);

                when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudBase));
                when(prioridadReglasService.calcularPrioridad(any(TipoSolicitud.class), any(LocalDateTime.class)))
                                .thenReturn(NivelPrioridad.ALTA);
                when(prioridadReglasService.generarJustificacion(any(TipoSolicitud.class), any(NivelPrioridad.class),
                                any(LocalDateTime.class)))
                                .thenReturn("Justificacion generada");
                when(authService.getUsuarioAutenticado()).thenReturn(ejecutor);
                when(solicitudRepository.save(any(SolicitudAcademica.class))).thenReturn(solicitudBase);

                SolicitudResponse response = solicitudService.clasificarSolicitud(request, 1L);

                assertNotNull(response);
                verify(solicitudRepository, atLeastOnce()).save(any());
        }

        @Test
        void clasificarSolicitud_deberiaFallarSiNoEstaRegistrada() {
                solicitudBase.setEstado(EstadoSolicitud.CLASIFICADA);

                ClasificarSolicitudRequest request = new ClasificarSolicitudRequest();
                request.setTipoSolicitud(TipoSolicitud.HOMOLOGACION);
                request.setVersion(0);

                when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudBase));

                assertThrows(InvalidTransitionException.class,
                                () -> solicitudService.clasificarSolicitud(request, 1L));
        }

        @Test
        void clasificarSolicitud_deberiaFallarSiSolicitudNoExiste() {
                when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

                ClasificarSolicitudRequest request = new ClasificarSolicitudRequest();
                request.setTipoSolicitud(TipoSolicitud.HOMOLOGACION);
                request.setVersion(0);

                assertThrows(RecursoNoEncontradoException.class,
                                () -> solicitudService.clasificarSolicitud(request, 99L));
        }

        // RF-04: Asignar Responsable
        @Test
        void asignarResponsable_deberiaTransicionarAEnAtencion() {
                solicitudBase.setEstado(EstadoSolicitud.CLASIFICADA);

                AsignarResponsableRequest request = new AsignarResponsableRequest();
                request.setResponsableId(3L);
                request.setVersion(0);

                Asignacion asignacion = Asignacion.builder()
                                .solicitud(solicitudBase)
                                .responsable(responsable)
                                .fechaAsignacion(LocalDateTime.now())
                                .activa(true)
                                .build();

                when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudBase));
                when(usuarioRepository.findById(3L)).thenReturn(Optional.of(responsable));
                when(authService.getUsuarioAutenticado()).thenReturn(ejecutor);
                when(asignacionRepository.save(any(Asignacion.class))).thenReturn(asignacion);
                when(solicitudRepository.save(any(SolicitudAcademica.class))).thenReturn(solicitudBase);

                SolicitudResponse response = solicitudService.asignarResponsable(request, 1L);

                assertNotNull(response);
                assertEquals(EstadoSolicitud.EN_ATENCION, response.getEstado());
                verify(asignacionRepository, times(1)).save(any());
        }

        @Test
        void asignarResponsable_deberiaFallarSiNoEstaClasificada() {
                solicitudBase.setEstado(EstadoSolicitud.REGISTRADA);

                AsignarResponsableRequest request = new AsignarResponsableRequest();
                request.setResponsableId(3L);
                request.setVersion(0);

                when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudBase));

                assertThrows(InvalidTransitionException.class,
                                () -> solicitudService.asignarResponsable(request, 1L));
        }

        @Test
        void asignarResponsable_deberiaFallarSiResponsableNoExiste() {
                solicitudBase.setEstado(EstadoSolicitud.CLASIFICADA);

                AsignarResponsableRequest request = new AsignarResponsableRequest();
                request.setResponsableId(99L);
                request.setVersion(0);

                when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudBase));
                when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

                assertThrows(RecursoNoEncontradoException.class,
                                () -> solicitudService.asignarResponsable(request, 1L));
        }

        @Test
        void asignarResponsable_solicitudYaEnAtencion_debeFallar() {
                solicitudBase.setEstado(EstadoSolicitud.EN_ATENCION);
                when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudBase));

                AsignarResponsableRequest request = new AsignarResponsableRequest();
                request.setResponsableId(3L);
                request.setVersion(0);

                assertThrows(InvalidTransitionException.class,
                                () -> solicitudService.asignarResponsable(request, 1L));
        }

        // RF-04: Atender Solicitud
        @Test
        void atenderSolicitud_deberiaTransicionarAAtendida() {
                solicitudBase.setEstado(EstadoSolicitud.EN_ATENCION);

                AtenderSolicitudRequest request = new AtenderSolicitudRequest();
                request.setObservacion("Se aprobo la homologacion");
                request.setVersion(0);

                when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudBase));
                when(authService.getUsuarioAutenticado()).thenReturn(ejecutor);
                when(solicitudRepository.save(any(SolicitudAcademica.class))).thenReturn(solicitudBase);

                SolicitudResponse response = solicitudService.atenderSolicitud(request, 1L);

                assertNotNull(response);
                assertEquals(EstadoSolicitud.ATENDIDA, response.getEstado());
        }

        @Test
        void atenderSolicitud_deberiaFallarSiNoEstaEnAtencion() {
                solicitudBase.setEstado(EstadoSolicitud.CLASIFICADA);

                AtenderSolicitudRequest request = new AtenderSolicitudRequest();
                request.setObservacion("Observación");
                request.setVersion(0);

                when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudBase));

                assertThrows(InvalidTransitionException.class,
                                () -> solicitudService.atenderSolicitud(request, 1L));
        }

        @Test
        void atenderSolicitud_solicitudYaAtendida_debeFallar() {
                solicitudBase.setEstado(EstadoSolicitud.ATENDIDA);
                when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudBase));

                AtenderSolicitudRequest request = new AtenderSolicitudRequest();
                request.setObservacion("Observación");
                request.setVersion(0);

                assertThrows(InvalidTransitionException.class,
                                () -> solicitudService.atenderSolicitud(request, 1L));
        }

        // RF-08: Cerrar Solicitud
        @Test
        void cerrarSolicitud_deberiaTransicionarACerrada() {
                solicitudBase.setEstado(EstadoSolicitud.ATENDIDA);

                CerrarSolicitudRequest request = new CerrarSolicitudRequest();
                request.setObservacionCierre("Proceso finalizado");
                request.setVersion(0);

                when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudBase));
                when(authService.getUsuarioAutenticado()).thenReturn(ejecutor);
                when(solicitudRepository.save(any(SolicitudAcademica.class))).thenReturn(solicitudBase);

                SolicitudResponse response = solicitudService.cerrarSolicitud(request, 1L);

                assertNotNull(response);
                assertEquals(EstadoSolicitud.CERRADA, response.getEstado());
        }

        @Test
        void cerrarSolicitud_deberiaFallarSiNoEstaAtendida() {
                solicitudBase.setEstado(EstadoSolicitud.EN_ATENCION);

                CerrarSolicitudRequest request = new CerrarSolicitudRequest();
                request.setObservacionCierre("Cierre");
                request.setVersion(0);

                when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudBase));

                assertThrows(InvalidTransitionException.class,
                                () -> solicitudService.cerrarSolicitud(request, 1L));
        }

        @Test
        void cerrarSolicitud_solicitudYaCerrada_debeFallar() {
                solicitudBase.setEstado(EstadoSolicitud.CERRADA);
                when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudBase));

                CerrarSolicitudRequest request = new CerrarSolicitudRequest();
                request.setObservacionCierre("Cierre");
                request.setVersion(0);

                assertThrows(InvalidTransitionException.class,
                                () -> solicitudService.cerrarSolicitud(request, 1L));
        }

        // RF-07: Consultar Solicitudes
        @Test
        void consultarSolicitudes_deberiaRetornarSolicitudesFiltradas() {
                Pageable pageable = PageRequest.of(0, 10);
                List<SolicitudAcademica> solicitudes = List.of(solicitudBase);
                Page<SolicitudAcademica> page = new PageImpl<>(solicitudes, pageable, 1);

                when(solicitudRepository.findByFiltros(
                                eq(EstadoSolicitud.REGISTRADA),
                                eq(null),
                                eq(null),
                                eq(null),
                                eq(pageable)))
                                .thenReturn(page);

                Page<SolicitudResponse> result = solicitudService.consultarSolicitudes(
                                EstadoSolicitud.REGISTRADA, null, null, null, pageable);

                assertNotNull(result);
                assertEquals(1, result.getTotalElements());
                assertEquals(1, result.getContent().size());

                verify(solicitudRepository, times(1)).findByFiltros(
                                any(), any(), any(), any(), any());
        }
}