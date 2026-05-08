package co.edu.uniquindio.triage_academico.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

public interface SolicitudService {
    SolicitudResponse crearSolicitud(CrearSolicitudRequest request, Long usuarioId);

    SolicitudResponse editarSolicitud(Long id, EditarSolicitudRequest request);

    SolicitudResponse obtenerPorId(Long id);

    SolicitudResponse aplicarSugerencia(Long id);

    SolicitudResponse clasificarSolicitud(ClasificarSolicitudRequest request, Long id);

    SolicitudResponse asignarResponsable(AsignarResponsableRequest request, Long id);

    SolicitudResponse atenderSolicitud(AtenderSolicitudRequest request, Long id);

    SolicitudResponse cerrarSolicitud(CerrarSolicitudRequest request, Long id);

    Page<SolicitudResponse> consultarSolicitudes(EstadoSolicitud estado, TipoSolicitud tipoSolicitud,
            NivelPrioridad nivelPrioridad, Long responsableId, Pageable pageable);
}