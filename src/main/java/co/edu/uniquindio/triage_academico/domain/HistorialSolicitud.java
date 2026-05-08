package co.edu.uniquindio.triage_academico.domain;

import java.time.LocalDateTime;

import co.edu.uniquindio.triage_academico.domain.enums.AccionHistorial;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "historial_solicitudes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudAcademica solicitud;

    @Column(name = "fecha_hora_accion", nullable = false)
    private LocalDateTime fechaHoraAccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AccionHistorial accion;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 1000)
    private String observacion;
}