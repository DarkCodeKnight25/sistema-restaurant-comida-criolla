package pe.dvega.restaurant.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.dvega.restaurant.dominio.ItemPedido;

public interface ItemPedidoRepositorio extends JpaRepository<ItemPedido, Long> {
}
