package co.edu.uniquindio.triage_academico.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;

@Component
public class PrioridadReglasService {

    // Mapa de prioridad por tipo de solicitud
    private static final Map<TipoSolicitud, NivelPrioridad> PRIORIDAD_POR_TIPO = Map.of(
        TipoSolicitud.HOMOLOGACION, NivelPrioridad.ALTA,
        TipoSolicitud.CANCELACION, NivelPrioridad.ALTA,
        TipoSolicitud.CUPOS, NivelPrioridad.ALTA,
        TipoSolicitud.REGISTRO_ASIGNATURAS, NivelPrioridad.MEDIA,
        TipoSolicitud.CONSULTA, NivelPrioridad.BAJA,
        TipoSolicitud.OTRO, NivelPrioridad.BAJA
    );

    public NivelPrioridad calcularPrioridad(TipoSolicitud tipo, LocalDateTime fechaLimite) {
        // 1. Prioridad por fecha limite
        if (fechaLimite != null) {
            long dias = LocalDateTime.now().until(fechaLimite, java.time.temporal.ChronoUnit.DAYS);
            if (dias <= 3) {
                return NivelPrioridad.CRITICA;
            }
            if (dias <= 7) {
                return NivelPrioridad.ALTA;
            }
        }
        
        // 2. Prioridad por tipo de solicitud
        return PRIORIDAD_POR_TIPO.getOrDefault(tipo, NivelPrioridad.MEDIA);
    }

    public String generarJustificacion(TipoSolicitud tipo, NivelPrioridad prioridad, LocalDateTime fechaLimite) {
        StringBuilder just = new StringBuilder();
        just.append("Prioridad ").append(prioridad).append(" asignada. ");
        
        // Justificacion por tipo
        switch (tipo) {
            case HOMOLOGACION:
                just.append("Las homologaciones requieren atencion prioritaria por impacto en el plan de estudios. ");
                break;
            case CANCELACION:
                just.append("Cancelaciones tienen fechas limite academicas estrictas. ");
                break;
            case CUPOS:
                just.append("Solicitudes de cupo afectan directamente la matricula del estudiante. ");
                break;
            case REGISTRO_ASIGNATURAS:
                just.append("Registro de asignaturas debe gestionarse antes del cierre de matriculas. ");
                break;
            case CONSULTA:
                just.append("Consulta academica de baja prioridad. ");
                break;
            default:
                break;
        }
        
        // Justificacion por fecha limite
        if (fechaLimite != null) {
            long dias = LocalDateTime.now().until(fechaLimite, java.time.temporal.ChronoUnit.DAYS);
            if (dias <= 3) {
                just.append("Fecha limite a menos de 3 dias. Impacto academico critico. ");
            } else if (dias <= 7) {
                just.append("Fecha limite a menos de 7 dias. Requiere atencion prioritaria. ");
            }
        }
        
        return just.toString();
    }
}