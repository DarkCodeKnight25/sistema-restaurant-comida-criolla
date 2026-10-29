package pe.dvega.restaurant.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.dvega.restaurant.dominio.CierreCaja;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CierreCajaRepositorio extends JpaRepository<CierreCaja, Long> {

    Optional<CierreCaja> findByFecha(LocalDate fecha);

    List<CierreCaja> findTop14ByOrderByFechaDesc();
}
