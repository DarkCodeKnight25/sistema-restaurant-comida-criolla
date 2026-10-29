package pe.dvega.restaurant.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.dvega.restaurant.dominio.Categoria;

import java.util.List;

public interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByOrden();
}
