package co.edu.uniquindio.triage_academico.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import co.edu.uniquindio.triage_academico.domain.enums.CanalOrigen;
import co.edu.uniquindio.triage_academico.domain.enums.EstadoSolicitud;
import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
import co.edu.uniquindio.triage_academico.exception.InvalidTransitionException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "solicitudes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudAcademica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitud estado;

    @Enumerated(EnumType.STRING)
    private NivelPrioridad nivelPrioridad;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    @Column(nullable = true)
    private LocalDateTime fechaLimite;

    @Column(nullable = true)
    private LocalDateTime fechaCierre;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("fechaHoraAccion ASC")
    @Builder.Default
    private List<HistorialSolicitud> historial = new ArrayList<>();

    @Column(length = 1000)
    private String observacionCierre;

    @Enumerated(EnumType.STRING)
    private TipoSolicitud tipoSolicitud;

    @Column
    private Long solicitanteId;

    @Column
    private Long responsableId;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Asignacion> asignaciones = new HashSet<>();

    @Column(length = 500)
    private String justificacionPrioridad;

    @Enumerated(EnumType.STRING)
    private CanalOrigen canalOrigen;

    @Column(length = 500)
    private String motivoRechazo;

    @Column(nullable = true)
    private LocalDateTime fechaResolucion;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public void clasificar(TipoSolicitud tipo, NivelPrioridad prioridad, String justificacion,
            LocalDateTime fechaLimite) {
        if (this.estado != EstadoSolicitud.REGISTRADA) {
            throw new InvalidTransitionException("Solo se puede clasificar en estado REGISTRADA");
        }
        this.tipoSolicitud = tipo;
        this.nivelPrioridad = prioridad;
        this.justificacionPrioridad = justificacion;
        this.fechaLimite = fechaLimite;
        this.estado = EstadoSolicitud.CLASIFICADA;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public void asignarResponsable(Usuario responsable) {
        if (this.estado != EstadoSolicitud.CLASIFICADA) {
            throw new InvalidTransitionException("Solo se puede asignar responsable en estado CLASIFICADA");
        }
        this.responsableId = responsable.getId();
        this.estado = EstadoSolicitud.EN_ATENCION;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public void atender() {
        if (this.estado != EstadoSolicitud.EN_ATENCION) {
            throw new InvalidTransitionException("Solo se puede atender en estado EN_ATENCION");
        }
        this.estado = EstadoSolicitud.ATENDIDA;
        this.fechaResolucion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }

    public void cerrar(String observacion) {
        if (this.estado != EstadoSolicitud.ATENDIDA) {
            throw new InvalidTransitionException("Solo se puede cerrar en estado ATENDIDA");
        }
        this.estado = EstadoSolicitud.CERRADA;
        this.observacionCierre = observacion;
        this.fechaCierre = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }
}
