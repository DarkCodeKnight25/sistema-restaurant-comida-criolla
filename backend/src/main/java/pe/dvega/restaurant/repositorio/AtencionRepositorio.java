package pe.dvega.restaurant.repositorio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.dvega.restaurant.dominio.Atencion;

import java.util.List;
import java.util.Optional;

public interface AtencionRepositorio extends JpaRepository<Atencion, Long> {

    @Query("select a from Atencion a where a.mesa.id = :mesaId and a.estado <> pe.dvega.restaurant.dominio.Atencion.Estado.PAGADA")
    Optional<Atencion> activa(@Param("mesaId") Long mesaId);

    @EntityGraph(attributePaths = {"mesa"})
    @Query("select a from Atencion a where a.estado <> pe.dvega.restaurant.dominio.Atencion.Estado.PAGADA")
    List<Atencion> activas();

    /** Bloquea la atención mientras se cobra, para que dos cajeros no cobren la misma mesa. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Atencion a where a.id = :id")
    Optional<Atencion> bloquear(@Param("id") Long id);
}
