package co.edu.uniquindio.triage_academico.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.triage_academico.domain.Asignacion;
import co.edu.uniquindio.triage_academico.domain.HistorialSolicitud;
import co.edu.uniquindio.triage_academico.domain.SolicitudAcademica;
import co.edu.uniquindio.triage_academico.domain.SugerenciaIA;
import co.edu.uniquindio.triage_academico.domain.Usuario;
import co.edu.uniquindio.triage_academico.domain.enums.AccionHistorial;
import co.edu.uniquindio.triage_academico.domain.enums.EstadoSolicitud;
import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import co.edu.uniquindio.triage_academico.dto.request.AsignarResponsableRequest;
import co.edu.uniquindio.triage_academico.dto.request.AtenderSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.CerrarSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.ClasificarSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.CrearSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.EditarSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.response.HistorialSolicitudResponse;
import co.edu.uniquindio.triage_academico.dto.response.SolicitudResponse;
import co.edu.uniquindio.triage_academico.exception.IAException;
import co.edu.uniquindio.triage_academico.exception.InvalidTransitionException;
import co.edu.uniquindio.triage_academico.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.triage_academico.exception.ReglaNegocioException;
import co.edu.uniquindio.triage_academico.repository.AsignacionRepository;
import co.edu.uniquindio.triage_academico.repository.SolicitudRepository;
import co.edu.uniquindio.triage_academico.repository.SugerenciaIARepository;
import co.edu.uniquindio.triage_academico.repository.UsuarioRepository;
import co.edu.uniquindio.triage_academico.service.AuthService;
import co.edu.uniquindio.triage_academico.service.PrioridadReglasService;
import co.edu.uniquindio.triage_academico.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudServiceImpl implements SolicitudService {
        private final SolicitudRepository solicitudRepository;
        private final SugerenciaIARepository sugerenciaIARepository;
        private final PrioridadReglasService prioridadReglasService;
        private final AsignacionRepository asignacionRepository;
        private final UsuarioRepository usuarioRepository;
        private final AuthService authService;

        // RF-01: Crear Solicitud
        @Override
        @Transactional
        public SolicitudResponse crearSolicitud(CrearSolicitudRequest request, Long usuarioId) {
                log.info("Creando solicitud para usuario: {}", usuarioId);

                SolicitudAcademica solicitud = SolicitudAcademica.builder()
                                .descripcion(request.getDescripcion())
                                .tipoSolicitud(request.getTipoSolicitud())
                                .solicitanteId(usuarioId)
                                .canalOrigen(request.getCanalOrigen())
                                .estado(EstadoSolicitud.REGISTRADA)
                                .nivelPrioridad(NivelPrioridad.MEDIA)
                                .fechaCreacion(LocalDateTime.now())
                                .historial(new ArrayList<>())
                                .version(0)
                                .build();

                solicitud = solicitudRepository.save(solicitud);

                Usuario ejecutor = authService.getUsuarioAutenticado();
                registrarHistorial(solicitud, AccionHistorial.REGISTRO, usuarioId, "Solicitud academica registrada.");

                solicitudRepository.save(solicitud);
                return mapToResponse(solicitud);
        }

        // Editar solicitud solo en estado REGISTRADA
        @Override
        @Transactional
        public SolicitudResponse editarSolicitud(Long id, EditarSolicitudRequest request) {
                log.info("Editando solicitud: {}", id);

                SolicitudAcademica solicitud = solicitudRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud", id));

                obtenerSolicitudConVersion(id, request.getVersion());

                if (solicitud.getEstado() != EstadoSolicitud.REGISTRADA) {
                        throw new InvalidTransitionException(
                                        "Solo se pueden editar solicitudes en estado REGISTRADA. Estado actual: "
                                                        + solicitud.getEstado());
                }

                String cambios = String.format("Descripción: '%s' → '%s', Tipo: %s → %s, Canal: %s → %s",
                                solicitud.getDescripcion(), request.getDescripcion(),
                                solicitud.getTipoSolicitud(), request.getTipoSolicitud(),
                                solicitud.getCanalOrigen(), request.getCanalOrigen());

                solicitud.setDescripcion(request.getDescripcion());
                solicitud.setTipoSolicitud(request.getTipoSolicitud());
                solicitud.setCanalOrigen(request.getCanalOrigen());
                solicitud.setFechaActualizacion(LocalDateTime.now());

                Usuario ejecutor = authService.getUsuarioAutenticado();
                registrarHistorial(solicitud, AccionHistorial.EDICION, ejecutor.getId(), cambios);

                solicitudRepository.save(solicitud);
                return mapToResponse(solicitud);
        }

        // Obtener por ID con historial (RF-06)
        @Override
        @Transactional(readOnly = true)
        public SolicitudResponse obtenerPorId(Long id) {
                SolicitudAcademica solicitud = solicitudRepository.findWithHistorialById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Solicitud no encontrada con id: " + id));
                return mapToResponse(solicitud);
        }

        // RF-10: Aplicar sugerencia de IA
        @Override
        @Transactional
        public SolicitudResponse aplicarSugerencia(Long id) {
                log.info("Aplicando sugerencia de IA para solicitud id: {}", id);

                SolicitudAcademica solicitud = solicitudRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud", id));

                if (solicitud.getEstado() != EstadoSolicitud.REGISTRADA) {
                        throw new InvalidTransitionException(
                                        "Solo se puede clasificar una solicitud en estado REGISTRADA");
                }

                SugerenciaIA sugerencia = sugerenciaIARepository
                                .findFirstBySolicitudIdOrderByFechaSugerenciaDesc(id)
                                .orElseThrow(() -> new IAException(
                                                "No hay sugerencia de IA pendiente para esta solicitud"));

                solicitud.setTipoSolicitud(sugerencia.getTipoSugerido());
                solicitud.setNivelPrioridad(sugerencia.getPrioridadSugerida());
                solicitud.setJustificacionPrioridad(sugerencia.getExplicacion());
                solicitud.setEstado(EstadoSolicitud.CLASIFICADA);
                solicitud.setFechaActualizacion(LocalDateTime.now());

                sugerencia.setAplicada(true);
                sugerenciaIARepository.save(sugerencia);

                Usuario ejecutor = authService.getUsuarioAutenticado();
                registrarHistorial(solicitud, AccionHistorial.CLASIFICACION, ejecutor.getId(),
                                String.format("Clasificacion aplicada usando sugerencia de IA. Tipo: %s, Prioridad: %s. Justificacion IA: %s",
                                                sugerencia.getTipoSugerido(), sugerencia.getPrioridadSugerida(),
                                                sugerencia.getExplicacion()));

                solicitudRepository.save(solicitud);
                return mapToResponse(solicitud);
        }

        // RF-02: Clasificar Solicitud y RF-03: Priorizacion de Solicitudes
        @Override
        @Transactional
        public SolicitudResponse clasificarSolicitud(ClasificarSolicitudRequest request, Long id) {
                log.info("Clasificando solicitud: {}", id);

                obtenerSolicitudConVersion(id, request.getVersion());

                SolicitudAcademica solicitud = solicitudRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Solicitud no encontrada con id: " + id));

                if (solicitud.getEstado() != EstadoSolicitud.REGISTRADA) {
                        throw new InvalidTransitionException(
                                        "Solo se puede clasificar una solicitud en estado REGISTRADA. Estado actual: "
                                                        + solicitud.getEstado());
                }

                // RF-03
                NivelPrioridad prioridadCalculada = prioridadReglasService.calcularPrioridad(
                                request.getTipoSolicitud(), request.getFechaLimite());
                String justificacionGenerada = prioridadReglasService.generarJustificacion(
                                request.getTipoSolicitud(), prioridadCalculada, request.getFechaLimite());

                solicitud.setTipoSolicitud(request.getTipoSolicitud());
                solicitud.setNivelPrioridad(prioridadCalculada);
                solicitud.setJustificacionPrioridad(justificacionGenerada);
                solicitud.setFechaLimite(request.getFechaLimite());
                solicitud.setEstado(EstadoSolicitud.CLASIFICADA);
                solicitud.setFechaActualizacion(LocalDateTime.now());

                Usuario ejecutor = authService.getUsuarioAutenticado();

                String observacionHistorial = String.format("Tipo: %s. Prioridad: %s. %s",
                                request.getTipoSolicitud(), prioridadCalculada, justificacionGenerada);
                if (observacionHistorial.length() > 1000) {
                        observacionHistorial = observacionHistorial.substring(0, 1000);
                }
                registrarHistorial(solicitud, AccionHistorial.CLASIFICACION, ejecutor.getId(), observacionHistorial);

                solicitudRepository.save(solicitud);

                return mapToResponse(solicitud);
        }

        // RF-04: CLASIFICADA a EN_ATENCION
        @Override
        @Transactional
        public SolicitudResponse asignarResponsable(AsignarResponsableRequest request, Long id) {
                log.info("Asignando responsable a solicitud: {}", id);

                obtenerSolicitudConVersion(id, request.getVersion());

                SolicitudAcademica solicitud = solicitudRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada: " + id));

                if (solicitud.getEstado() != EstadoSolicitud.CLASIFICADA) {
                        throw new InvalidTransitionException(
                                        "Solo se puede asignar responsable a una solicitud en estado CLASIFICADA");
                }

                Usuario responsable = usuarioRepository.findById(request.getResponsableId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Usuario responsable no encontrado: " + request.getResponsableId()));

                Asignacion asignacion = Asignacion.builder()
                                .solicitud(solicitud)
                                .responsable(responsable)
                                .fechaAsignacion(LocalDateTime.now())
                                .activa(true)
                                .build();

                asignacion = asignacionRepository.save(asignacion);

                if (solicitud.getAsignaciones() == null) {
                        solicitud.setAsignaciones(new HashSet<>());
                }
                solicitud.getAsignaciones().add(asignacion);

                solicitud.setEstado(EstadoSolicitud.EN_ATENCION);
                solicitud.setFechaActualizacion(LocalDateTime.now());

                Usuario ejecutor = authService.getUsuarioAutenticado();

                registrarHistorial(solicitud, AccionHistorial.ASIGNACION_RESPONSABLE, ejecutor.getId(),
                                "Responsable asignado: id " + request.getResponsableId());

                solicitudRepository.save(solicitud);

                return mapToResponse(solicitud);
        }

        // RF-04: EN_ATENCION a ATENDIDA
        @Override
        @Transactional
        public SolicitudResponse atenderSolicitud(AtenderSolicitudRequest request, Long id) {

                obtenerSolicitudConVersion(id, request.getVersion());

                SolicitudAcademica solicitud = solicitudRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Solicitud no encontrada con id: " + id));

                if (solicitud.getEstado() != EstadoSolicitud.EN_ATENCION) {
                        throw new InvalidTransitionException(
                                        "Solo se puede atender una solicitud en estado EN_ATENCION. Estado actual: "
                                                        + solicitud.getEstado());
                }

                solicitud.setEstado(EstadoSolicitud.ATENDIDA);
                solicitud.setFechaActualizacion(LocalDateTime.now());
                solicitud.setFechaResolucion(LocalDateTime.now());

                Usuario ejecutor = authService.getUsuarioAutenticado();

                registrarHistorial(solicitud, AccionHistorial.ATENCION, ejecutor.getId(), request.getObservacion());

                solicitudRepository.save(solicitud);

                return mapToResponse(solicitud);
        }

        // RF-04 y RF-08: ATENDIDA a CERRADA
        @Override
        @Transactional
        public SolicitudResponse cerrarSolicitud(CerrarSolicitudRequest request, Long id) {

                obtenerSolicitudConVersion(id, request.getVersion());

                SolicitudAcademica solicitud = solicitudRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Solicitud no encontrada con id: " + id));

                if (solicitud.getEstado() != EstadoSolicitud.ATENDIDA) {
                        throw new InvalidTransitionException(
                                        "Solo se puede cerrar una solicitud en estado ATENDIDA. Estado actual: "
                                                        + solicitud.getEstado());
                }

                solicitud.setEstado(EstadoSolicitud.CERRADA);
                solicitud.setObservacionCierre(request.getObservacionCierre());
                solicitud.setFechaCierre(LocalDateTime.now());
                solicitud.setFechaActualizacion(LocalDateTime.now());

                Usuario ejecutor = authService.getUsuarioAutenticado();

                registrarHistorial(solicitud, AccionHistorial.CIERRE, ejecutor.getId(), request.getObservacionCierre());

                solicitudRepository.save(solicitud);

                return mapToResponse(solicitud);
        }

        // Metodos privados auxiliares
        private void registrarHistorial(SolicitudAcademica solicitud, AccionHistorial accion, Long usuarioId,
                        String observacion) {
                if (solicitud.getHistorial() == null) {
                        solicitud.setHistorial(new ArrayList<>());
                }
                solicitud.getHistorial().add(HistorialSolicitud.builder()
                                .solicitud(solicitud)
                                .fechaHoraAccion(LocalDateTime.now())
                                .accion(accion)
                                .usuarioId(usuarioId)
                                .observacion(observacion)
                                .build());
        }

        private List<HistorialSolicitudResponse> mapHistorial(SolicitudAcademica solicitud) {
                if (solicitud.getHistorial() == null || solicitud.getHistorial().isEmpty()) {
                        return List.of();
                }
                return solicitud.getHistorial().stream()
                                .map(h -> HistorialSolicitudResponse.builder()
                                                .id(h.getId())
                                                .fechaHoraAccion(h.getFechaHoraAccion())
                                                .accion(h.getAccion())
                                                .usuarioId(h.getUsuarioId())
                                                .observacion(h.getObservacion())
                                                .build())
                                .toList();
        }

        private SolicitudAcademica obtenerSolicitudConVersion(Long id, Integer versionCliente) {
                SolicitudAcademica solicitud = solicitudRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud", id));

                if (!solicitud.getVersion().equals(versionCliente)) {
                        throw new ReglaNegocioException(
                                        "Conflicto de concurrencia: version esperada " + versionCliente
                                                        + ", version actual " + solicitud.getVersion());
                }
                return solicitud;
        }

        // RF-07: Consulta de Solicitudes con filtros
        @Override
        @Transactional(readOnly = true)
        public Page<SolicitudResponse> consultarSolicitudes(EstadoSolicitud estado, TipoSolicitud tipoSolicitud,
                        NivelPrioridad nivelPrioridad, Long responsableId, Pageable pageable) {
                Page<SolicitudAcademica> solicitudesPage = solicitudRepository.findByFiltros(estado, tipoSolicitud,
                                nivelPrioridad, responsableId, pageable);

                List<SolicitudResponse> responses = solicitudesPage.getContent().stream().map(this::mapToResponse)
                                .toList();

                return new PageImpl<>(responses, pageable, solicitudesPage.getTotalElements());
        }

        private SolicitudResponse mapToResponse(SolicitudAcademica solicitud) {
                Asignacion asignacionActiva = (solicitud.getAsignaciones() != null
                                ? solicitud.getAsignaciones().stream()
                                                .filter(Asignacion::isActiva)
                                                .findFirst()
                                                .orElse(null)
                                : null);

                return SolicitudResponse.builder()
                                .id(solicitud.getId())
                                .descripcion(solicitud.getDescripcion())
                                .estado(solicitud.getEstado())
                                .nivelPrioridad(solicitud.getNivelPrioridad())
                                .tipoSolicitud(solicitud.getTipoSolicitud())
                                .solicitanteId(solicitud.getSolicitanteId())
                                .responsableId(asignacionActiva != null ? asignacionActiva.getResponsable().getId()
                                                : null)
                                .responsableNombre(
                                                asignacionActiva != null ? asignacionActiva.getResponsable().getNombre()
                                                                : null)
                                .responsableApellido(asignacionActiva != null
                                                ? asignacionActiva.getResponsable().getApellido()
                                                : null)
                                .fechaCreacion(solicitud.getFechaCreacion())
                                .fechaActualizacion(solicitud.getFechaActualizacion())
                                .justificacionPrioridad(solicitud.getJustificacionPrioridad())
                                .fechaLimite(solicitud.getFechaLimite())
                                .fechaCierre(solicitud.getFechaCierre())
                                .fechaResolucion(solicitud.getFechaResolucion())
                                .observacionCierre(solicitud.getObservacionCierre())
                                .historial(mapHistorial(solicitud))
                                .build();
        }
}
