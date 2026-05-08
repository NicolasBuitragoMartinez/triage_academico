package co.edu.uniquindio.triage_academico.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.uniquindio.triage_academico.domain.SolicitudAcademica;
import co.edu.uniquindio.triage_academico.domain.enums.EstadoSolicitud;
import co.edu.uniquindio.triage_academico.domain.enums.NivelPrioridad;
import co.edu.uniquindio.triage_academico.domain.enums.TipoSolicitud;

@Repository
public interface SolicitudRepository extends JpaRepository<SolicitudAcademica, Long> {

        List<SolicitudAcademica> findByEstado(EstadoSolicitud estado);

        List<SolicitudAcademica> findByNivelPrioridad(NivelPrioridad prioridad);

        // Metodo que carga el historial y las asignaciones con sus responsables
        @EntityGraph(attributePaths = { "historial", "asignaciones", "asignaciones.responsable" })
        @Query("SELECT s FROM SolicitudAcademica s WHERE s.id = :id")
        Optional<SolicitudAcademica> findWithHistorialById(@Param("id") Long id);

        // Metodo para filtrar solicitudes por estado, tipo, prioridad y responsable
        @Query("SELECT DISTINCT s FROM SolicitudAcademica s " +
                        "LEFT JOIN s.asignaciones a ON a.activa = true " +
                        "WHERE (:estado IS NULL OR s.estado = :estado) " +
                        "AND (:tipoSolicitud IS NULL OR s.tipoSolicitud = :tipoSolicitud) " +
                        "AND (:nivelPrioridad IS NULL OR s.nivelPrioridad = :nivelPrioridad) " +
                        "AND (:responsableId IS NULL OR a.responsable.id = :responsableId)")
        Page<SolicitudAcademica> findByFiltros(@Param("estado") EstadoSolicitud estado,
                        @Param("tipoSolicitud") TipoSolicitud tipoSolicitud,
                        @Param("nivelPrioridad") NivelPrioridad nivelPrioridad,
                        @Param("responsableId") Long responsableId,
                        Pageable pageable);
}
