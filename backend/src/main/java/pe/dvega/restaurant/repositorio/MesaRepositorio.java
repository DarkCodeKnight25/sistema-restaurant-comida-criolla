package pe.dvega.restaurant.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.dvega.restaurant.dominio.Mesa;

import java.util.List;
import java.util.Optional;

public interface MesaRepositorio extends JpaRepository<Mesa, Long> {

    Optional<Mesa> findByToken(String token);

    List<Mesa> findAllByOrderByNumero();
}
