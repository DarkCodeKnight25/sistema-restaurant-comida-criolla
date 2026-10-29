package pe.dvega.restaurant.repositorio;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.dvega.restaurant.dominio.Plato;

import java.util.List;

public interface PlatoRepositorio extends JpaRepository<Plato, Long> {

    @EntityGraph(attributePaths = {"categoria", "estacion"})
    List<Plato> findByActivoTrueOrderByCategoriaOrdenAscOrdenAsc();

    @EntityGraph(attributePaths = {"categoria", "estacion"})
    List<Plato> findAllByOrderByCategoriaOrdenAscOrdenAsc();
}
