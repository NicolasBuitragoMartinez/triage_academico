package co.edu.uniquindio.triage_academico.service;

import co.edu.uniquindio.triage_academico.dto.response.ResumenSolicitudResponse;
import co.edu.uniquindio.triage_academico.dto.response.SugerenciaClasificacionResponse;

public interface IAService {
    // RF-10
    SugerenciaClasificacionResponse sugerirClasificacion(Long solicitudId, String descripcion);

    // RF-09
    ResumenSolicitudResponse generarResumen(Long solicitudId);
}
