package co.edu.uniquindio.triage_academico.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.triage_academico.domain.enums.EstadoSolicitud;
import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import co.edu.uniquindio.triage_academico.dto.request.AsignarResponsableRequest;
import co.edu.uniquindio.triage_academico.dto.request.AtenderSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.CerrarSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.ClasificarSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.CrearSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.request.EditarSolicitudRequest;
import co.edu.uniquindio.triage_academico.dto.response.SolicitudResponse;
import co.edu.uniquindio.triage_academico.service.SolicitudService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;

    // RF-06: Obtener solicitud con historial
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SolicitudResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.obtenerPorId(id));
    }

    // RF-01
    @PostMapping
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'ADMINISTRATIVO', 'COORDINADOR', 'DIRECTOR')")
    public ResponseEntity<SolicitudResponse> crearSolicitud(@Valid @RequestBody CrearSolicitudRequest request) {
        Long usuarioId = request.getSolicitanteId() != null ? request.getSolicitanteId() : 0L;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solicitudService.crearSolicitud(request, usuarioId));
    }

    // Edicion de solicitud solo en estado REGISTRADA
    @PutMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'ADMINISTRATIVO')")
    public ResponseEntity<SolicitudResponse> editarSolicitud(
            @PathVariable Long id,
            @Valid @RequestBody EditarSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.editarSolicitud(id, request));
    }

    @PostMapping("/{id}/aplicar-sugerencia")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'COORDINADOR', 'DIRECTOR')")
    public ResponseEntity<SolicitudResponse> aplicarSugerencia(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.aplicarSugerencia(id));
    }

    // RF-02 + RF-03
    @PatchMapping("/{id}/clasificar")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'COORDINADOR', 'DIRECTOR')")
    public ResponseEntity<SolicitudResponse> clasificar(
            @PathVariable Long id,
            @Valid @RequestBody ClasificarSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.clasificarSolicitud(request, id));
    }

    // RF-04: CLASIFICADA a EN_ATENCION
    @PatchMapping("/{id}/asignar")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'DIRECTOR')")
    public ResponseEntity<SolicitudResponse> asignarResponsable(
            @PathVariable Long id,
            @Valid @RequestBody AsignarResponsableRequest request) {
        return ResponseEntity.ok(solicitudService.asignarResponsable(request, id));
    }

    // RF-04: EN_ATENCION a ATENDIDA
    @PatchMapping("/{id}/atender")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'COORDINADOR', 'DIRECTOR')")
    public ResponseEntity<SolicitudResponse> atender(
            @PathVariable Long id,
            @Valid @RequestBody AtenderSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.atenderSolicitud(request, id));
    }

    // RF-04 + RF-08: ATENDIDA a CERRADA
    @PatchMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'COORDINADOR', 'DIRECTOR')")
    public ResponseEntity<SolicitudResponse> cerrar(
            @PathVariable Long id,
            @Valid @RequestBody CerrarSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.cerrarSolicitud(request, id));
    }

    // RF-07: Consulta de Solicitudes con filtros
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<SolicitudResponse>> consultarSolicitudes(
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) TipoSolicitud tipoSolicitud,
            @RequestParam(required = false) NivelPrioridad nivelPrioridad,
            @RequestParam(required = false) Long responsableId,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<SolicitudResponse> result = solicitudService.consultarSolicitudes(
                estado, tipoSolicitud, nivelPrioridad, responsableId, pageable);

        return ResponseEntity.ok(result);
    }
}