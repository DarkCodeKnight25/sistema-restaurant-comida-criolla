package pe.dvega.restaurant.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.dvega.restaurant.dominio.Estacion;

import java.util.List;

public interface EstacionRepositorio extends JpaRepository<Estacion, Long> {

    List<Estacion> findAllByOrderByOrden();
}
