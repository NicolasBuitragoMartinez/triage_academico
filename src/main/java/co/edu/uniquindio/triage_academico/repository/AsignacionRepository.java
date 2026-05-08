package co.edu.uniquindio.triage_academico.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import co.edu.uniquindio.triage_academico.domain.Asignacion;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {
    
    Optional<Asignacion> findBySolicitudIdAndActivaTrue(Long solicitudId);
}
