package pe.dvega.restaurant.repositorio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.dvega.restaurant.dominio.ContadorDia;

import java.time.LocalDate;
import java.util.Optional;

public interface ContadorDiaRepositorio extends JpaRepository<ContadorDia, LocalDate> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContadorDia c where c.fecha = :fecha")
    Optional<ContadorDia> bloquear(@Param("fecha") LocalDate fecha);
}
