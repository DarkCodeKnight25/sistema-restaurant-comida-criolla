package pe.dvega.restaurant.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.dvega.restaurant.config.LocalProps;
import pe.dvega.restaurant.dominio.*;
import pe.dvega.restaurant.repositorio.*;
import pe.dvega.restaurant.servicio.PedidoService;
import pe.dvega.restaurant.web.Dtos.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Lo que usa el comensal desde el QR de su mesa y el cliente de delivery. No pide sesión. */
@RestController
@RequestMapping("/api/publico")
public class PublicoController {

    private final PedidoService servicio;
    private final EstacionRepositorio estaciones;
    private final CategoriaRepositorio categorias;
    private final PlatoRepositorio platos;
    private final MesaRepositorio mesas;
    private final AtencionRepositorio atenciones;
    private final PedidoRepositorio pedidos;
    private final LocalProps local;
    private final Clock reloj;

    public PublicoController(PedidoService servicio, EstacionRepositorio estaciones, CategoriaRepositorio categorias,
                             PlatoRepositorio platos, MesaRepositorio mesas, AtencionRepositorio atenciones,
                             PedidoRepositorio pedidos, LocalProps local, Clock reloj) {
        this.servicio = servicio;
        this.estaciones = estaciones;
        this.categorias = categorias;
        this.platos = platos;
        this.mesas = mesas;
        this.atenciones = atenciones;
        this.pedidos = pedidos;
        this.local = local;
        this.reloj = reloj;
    }

    @GetMapping("/local")
    LocalDto local() {
        return new LocalDto(local.nombre(), local.tipo(), local.direccion(), local.distrito(), local.horario(),
                local.celular(), local.yapeDemo(), local.demo(), LocalDateTime.now(reloj));
    }

    @GetMapping("/carta")
    @Transactional(readOnly = true)
    CartaDto carta() {
        return new CartaDto(estaciones.findAllByOrderByOrden().stream().map(EstacionDto::de).toList(),
                categorias.findAllByOrderByOrden().stream().map(CategoriaDto::de).toList(),
                platos.findByActivoTrueOrderByCategoriaOrdenAscOrdenAsc().stream().map(PlatoDto::de).toList());
    }

    @GetMapping("/mesas/{token}")
    @Transactional(readOnly = true)
    MesaPublicaDto mesa(@PathVariable String token) {
        Mesa m = mesas.findByToken(token).orElseThrow(() -> new NoEncontradoException("Este QR no corresponde a ninguna mesa."));
        return atenciones.activa(m.getId())
                .map(a -> {
                    List<Pedido> lista = pedidos.findByAtencionIdOrderByCreadoEn(a.getId());
                    BigDecimal total = lista.stream().filter(p -> p.getEstado() != Pedido.Estado.ANULADO)
                            .map(Pedido::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new MesaPublicaDto(m.getNumero(), m.getZona(), a.getEstado(), total,
                            lista.stream().map(PedidoDto::publico).toList());
                })
                .orElseGet(() -> new MesaPublicaDto(m.getNumero(), m.getZona(), null, BigDecimal.ZERO, List.of()));
    }

    @PostMapping("/mesas/{token}/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    PedidoDto pedirEnMesa(@PathVariable String token, @Valid @RequestBody PedidoMesaIn in) {
        return PedidoDto.publico(servicio.pedirEnMesa(token, in.lineas().stream().map(LineaIn::aLinea).toList(), in.nota()));
    }

    @PostMapping("/mesas/{token}/cuenta")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void pedirCuenta(@PathVariable String token) {
        servicio.pedirCuenta(token);
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    PedidoDto pedirExterno(@Valid @RequestBody PedidoExternoIn in) {
        ClienteIn c = in.cliente();
        return PedidoDto.publico(servicio.pedirExterno(in.tipo(),
                new PedidoService.DatosCliente(c.nombre(), c.celular(), c.direccion(), c.referencia()),
                in.lineas().stream().map(LineaIn::aLinea).toList(), in.nota()));
    }

    @GetMapping("/pedidos/{codigo}")
    @Transactional(readOnly = true)
    PedidoDto seguimiento(@PathVariable String codigo) {
        Pedido p = pedidos.findByCodigo(codigo.toUpperCase())
                .filter(x -> x.getTipo() != Pedido.Tipo.MESA)
                .orElseThrow(() -> new NoEncontradoException("No encontramos el pedido " + codigo + "."));
        return PedidoDto.publico(p);
    }
}
