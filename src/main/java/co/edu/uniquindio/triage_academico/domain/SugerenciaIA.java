package co.edu.uniquindio.triage_academico.domain;

import java.time.LocalDateTime;

import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;
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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sugerencias_ia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SugerenciaIA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudAcademica solicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSolicitud tipoSugerido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelPrioridad prioridadSugerida;

    @Column(length = 1000)
    private String explicacion;

    @Column(nullable = false)
    private Float confianza;

    @Column(nullable = false)
    private LocalDateTime fechaSugerencia;

    @Column(nullable = false)
    private boolean aplicada = false;
}