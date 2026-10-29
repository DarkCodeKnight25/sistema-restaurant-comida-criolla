package pe.dvega.restaurant.repositorio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.dvega.restaurant.dominio.Pedido;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepositorio extends JpaRepository<Pedido, Long> {

    @Query("select coalesce(max(p.numero), 0) from Pedido p where p.fecha = :fecha")
    int ultimoNumero(@Param("fecha") LocalDate fecha);

    @EntityGraph(attributePaths = {"items", "atencion", "atencion.mesa"})
    List<Pedido> findByAtencionIdOrderByCreadoEn(Long atencionId);

    @EntityGraph(attributePaths = {"items", "atencion", "atencion.mesa"})
    List<Pedido> findByFechaAndEstadoInOrderByCreadoEn(LocalDate fecha, Collection<Pedido.Estado> estados);

    @EntityGraph(attributePaths = {"items", "atencion", "atencion.mesa"})
    List<Pedido> findByFechaOrderByCreadoEn(LocalDate fecha);

    @EntityGraph(attributePaths = {"items", "atencion", "atencion.mesa"})
    Optional<Pedido> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"items", "atencion", "atencion.mesa"})
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> detalle(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> bloquear(@Param("id") Long id);

    boolean existsByCodigo(String codigo);
}
