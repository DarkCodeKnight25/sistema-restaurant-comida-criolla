package pe.dvega.restaurant.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.dvega.restaurant.dominio.Usuario;

import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);
}
