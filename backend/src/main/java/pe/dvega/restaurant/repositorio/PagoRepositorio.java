package pe.dvega.restaurant.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.dvega.restaurant.dominio.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PagoRepositorio extends JpaRepository<Pago, Long> {

    List<Pago> findByCreadoEnBetweenOrderByCreadoEn(LocalDateTime desde, LocalDateTime hasta);

    @Query("select coalesce(sum(p.monto), 0) from Pago p where p.atencion.id = :atencionId")
    BigDecimal pagadoEnAtencion(@Param("atencionId") Long atencionId);

    @Query("select coalesce(sum(p.monto), 0) from Pago p where p.pedido.id = :pedidoId")
    BigDecimal pagadoEnPedido(@Param("pedidoId") Long pedidoId);
}
