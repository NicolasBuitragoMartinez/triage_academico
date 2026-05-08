package co.edu.uniquindio.triage_academico.controller;

import co.edu.uniquindio.triage_academico.dto.request.SugerenciaClasificacionRequest;
import co.edu.uniquindio.triage_academico.dto.response.ResumenSolicitudResponse;
import co.edu.uniquindio.triage_academico.dto.response.SugerenciaClasificacionResponse;
import co.edu.uniquindio.triage_academico.service.IAService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
public class IAController {

    private final IAService iaService;

    // RF-10
    @PostMapping("/sugerir-clasificacion")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'COORDINADOR', 'DIRECTOR')")
    public ResponseEntity<SugerenciaClasificacionResponse> sugerirClasificacion(
            @Valid @RequestBody SugerenciaClasificacionRequest request) {
        return ResponseEntity.ok(iaService.sugerirClasificacion(request.getSolicitudId(), request.getDescripcion()));
    }

    // RF-09
    @GetMapping("/resumen/{solicitudId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'COORDINADOR', 'DIRECTOR')")
    public ResponseEntity<ResumenSolicitudResponse> generarResumen(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(iaService.generarResumen(solicitudId));
    }
}