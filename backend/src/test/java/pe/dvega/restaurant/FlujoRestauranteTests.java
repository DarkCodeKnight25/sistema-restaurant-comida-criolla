package pe.dvega.restaurant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;
import pe.dvega.restaurant.demo.DatosDemo;
import pe.dvega.restaurant.dominio.*;
import pe.dvega.restaurant.repositorio.*;
import pe.dvega.restaurant.servicio.CajaService;
import pe.dvega.restaurant.servicio.PedidoService;
import pe.dvega.restaurant.servicio.PedidoService.Linea;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(RelojFijo.class)
class FlujoRestauranteTests {

    @Autowired PedidoService pedidos;
    @Autowired CajaService caja;
    @Autowired DatosDemo demo;
    @Autowired TransactionTemplate tx;
    @Autowired PlatoRepositorio platos;
    @Autowired PedidoRepositorio pedidoRepo;
    @Autowired AtencionRepositorio atenciones;

    @BeforeEach
    void sembrar() {
        tx.executeWithoutResult(s -> {
            demo.borrarTodo();
            demo.sembrar(20261029L);
        });
    }

    @AfterEach
    void limpiar() {
        tx.executeWithoutResult(s -> demo.borrarTodo());
    }

    @Test
    void elPrecioSaleDeLaCartaYNoDelNavegador() {
        Plato lomo = plato("Lomo saltado");
        Pedido p = pedidos.pedirEnMesa(DatosDemo.token(3), List.of(new Linea(lomo.getId(), 2, "sin cebolla")), null);

        assertThat(p.getTotal()).isEqualByComparingTo("64.00");
        assertThat(p.getEstado()).isEqualTo(Pedido.Estado.ENVIADO);
        assertThat(p.getItems().get(0).getNota()).isEqualTo("sin cebolla");
    }

    @Test
    void noSePuedePedirUnPlatoAgotado() {
        Plato tallarin = plato("Tallarín saltado");
        Plato lomo = plato("Lomo saltado");

        assertThatThrownBy(() -> pedidos.pedirEnMesa(DatosDemo.token(3),
                List.of(new Linea(lomo.getId(), 1, null), new Linea(tallarin.getId(), 1, null)), null))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Tallarín")
                .satisfies(e -> assertThat(((ConflictoException) e).getPlatosAgotados()).containsExactly(tallarin.getId()));
    }

    @Test
    void unQrInventadoNoAbreNingunaMesa() {
        assertThatThrownBy(() -> pedidos.pedirEnMesa("m3-adivinado", List.of(new Linea(plato("Lomo saltado").getId(), 1, null)), null))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void dosPedidosSimultaneosNuncaRepitenNumero() throws Exception {
        Long lomo = plato("Lomo saltado").getId();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> futuros = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int mesa = i + 5;
            futuros.add(pool.submit(() -> {
                salida.await();
                return pedidos.pedirEnMesa(DatosDemo.token(mesa), List.of(new Linea(lomo, 1, null)), null).getNumero();
            }));
        }
        salida.countDown();
        Set<Integer> numeros = new HashSet<>();
        for (Future<Integer> f : futuros) {
            numeros.add(f.get(20, TimeUnit.SECONDS));
        }
        pool.shutdown();
        assertThat(numeros).hasSize(4);
    }

    @Test
    void elPedidoSigueElEstadoDeSusPlatos() {
        Plato lomo = plato("Lomo saltado");
        Plato huancaina = plato("Papa a la huancaína");
        Pedido p = pedidos.pedirEnMesa(DatosDemo.token(3),
                List.of(new Linea(lomo.getId(), 1, null), new Linea(huancaina.getId(), 1, null)), null);

        assertThatThrownBy(() -> pedidos.avanzarItem(p.getItems().get(0).getId(), ItemPedido.Estado.LISTO))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("confirmar");

        pedidos.confirmar(p.getId());
        Pedido fogon = pedidos.estacionLista(p.getId(), lomo.getEstacion().getId());
        assertThat(fogon.getEstado()).isEqualTo(Pedido.Estado.EN_PREPARACION);

        Pedido todo = pedidos.estacionLista(p.getId(), huancaina.getEstacion().getId());
        assertThat(todo.getEstado()).isEqualTo(Pedido.Estado.LISTO);

        assertThat(pedidos.servirListos(p.getId()).getEstado()).isEqualTo(Pedido.Estado.SERVIDO);
    }

    @Test
    void laMesaSeLiberaCuandoSePagaTodoAunqueSeaEnPartes() {
        Plato bistec = plato("Bistec a lo pobre");
        Pedido p = pedidos.pedirEnMesa(DatosDemo.token(3), List.of(new Linea(bistec.getId(), 2, null)), null);
        Long atencionId = p.getAtencion().getId();

        assertThatThrownBy(() -> caja.cobrarMesa(atencionId, Pago.Medio.YAPE, new BigDecimal("10"), null, "Caja"))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("sin confirmar");

        pedidos.confirmar(p.getId());
        pedidos.pedirCuenta(DatosDemo.token(3));
        caja.cobrarMesa(atencionId, Pago.Medio.YAPE, new BigDecimal("60.00"), null, "Caja");
        assertThat(atenciones.findById(atencionId).orElseThrow().getEstado()).isEqualTo(Atencion.Estado.CUENTA);

        assertThatThrownBy(() -> caja.cobrarMesa(atencionId, Pago.Medio.EFECTIVO, new BigDecimal("50.00"), new BigDecimal("50"), "Caja"))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("mayor que el saldo");

        Pago efectivo = caja.cobrarMesa(atencionId, Pago.Medio.EFECTIVO, new BigDecimal("8.00"), new BigDecimal("10"), "Caja");
        assertThat(efectivo.getVuelto()).isEqualByComparingTo("2.00");
        assertThat(atenciones.findById(atencionId).orElseThrow().getEstado()).isEqualTo(Atencion.Estado.PAGADA);
    }

    @Test
    void laCajaNoCierraConMesasSinCobrarYCuadraLoCobrado() {
        assertThatThrownBy(() -> caja.cerrar(RelojFijo.AHORA.toLocalDate(), BigDecimal.ZERO, "Caja"))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("sin cobrar");

        CajaService.Resumen r = caja.resumen(RelojFijo.AHORA.toLocalDate());
        BigDecimal sumaMedios = r.porMedio().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumaMedios).isEqualByComparingTo(r.total());
        assertThat(r.tickets()).isPositive();
        assertThat(r.mesasAbiertas()).isEqualTo(5);
    }

    @Test
    void deliverySinDireccionNoSeAcepta() {
        assertThatThrownBy(() -> pedidos.pedirExterno(Pedido.Tipo.DELIVERY,
                new PedidoService.DatosCliente("Ana", "900000001", " ", null),
                List.of(new Linea(plato("Mazamorra morada").getId(), 1, null)), null))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("dirección");
    }

    @Test
    void unPedidoParaLlevarSoloSeEntregaCobrado() {
        Pedido p = pedidos.pedirExterno(Pedido.Tipo.LLEVAR, new PedidoService.DatosCliente("Ana", "900000001", null, null),
                List.of(new Linea(plato("Mazamorra morada").getId(), 1, null)), null);
        pedidos.confirmar(p.getId());
        pedidos.estacionLista(p.getId(), plato("Mazamorra morada").getEstacion().getId());

        assertThatThrownBy(() -> pedidos.entregar(p.getId())).isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Cobra");
        caja.cobrarPedido(p.getId(), Pago.Medio.PLIN, null, "Caja");
        assertThat(pedidos.entregar(p.getId()).getEstado()).isEqualTo(Pedido.Estado.ENTREGADO);
    }

    private Plato plato(String nombre) {
        return tx.execute(s -> platos.findAll().stream().filter(p -> p.getNombre().equals(nombre)).findFirst()
                .map(p -> {
                    p.getEstacion().getId();
                    return p;
                }).orElseThrow());
    }
}
