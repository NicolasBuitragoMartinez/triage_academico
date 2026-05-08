package co.edu.uniquindio.triage_academico.repository;

import co.edu.uniquindio.triage_academico.domain.SugerenciaIA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SugerenciaIARepository extends JpaRepository<SugerenciaIA, Long> {
    
    List<SugerenciaIA> findBySolicitudIdOrderByFechaSugerenciaDesc(Long solicitudId);
    
    Optional<SugerenciaIA> findFirstBySolicitudIdOrderByFechaSugerenciaDesc(Long solicitudId);

    Optional<SugerenciaIA> findFirstBySolicitudIdAndAplicadaFalseOrderByFechaSugerenciaDesc(Long solicitudId);
}