package co.edu.uniquindio.triage_academico.repository;

import co.edu.uniquindio.triage_academico.domain.Rol;
import co.edu.uniquindio.triage_academico.domain.enums.NombreRol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(NombreRol nombre);
}