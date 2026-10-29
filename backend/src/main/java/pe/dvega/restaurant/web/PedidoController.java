package pe.dvega.restaurant.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.dvega.restaurant.dominio.Pedido;
import pe.dvega.restaurant.repositorio.PedidoRepositorio;
import pe.dvega.restaurant.servicio.PedidoService;
import pe.dvega.restaurant.web.Dtos.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

/** Pedidos para el personal: cocina, mozos y caja. */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private static final EnumSet<Pedido.Estado> EN_CURSO = EnumSet.of(Pedido.Estado.ENVIADO, Pedido.Estado.CONFIRMADO,
            Pedido.Estado.EN_PREPARACION, Pedido.Estado.LISTO, Pedido.Estado.EN_CAMINO);

    private final PedidoService servicio;
    private final PedidoRepositorio pedidos;
    private final Clock reloj;

    public PedidoController(PedidoService servicio, PedidoRepositorio pedidos, Clock reloj) {
        this.servicio = servicio;
        this.pedidos = pedidos;
        this.reloj = reloj;
    }

    /** Pedidos de hoy que siguen en curso; con todos=true, también los cerrados. */
    @GetMapping
    @Transactional(readOnly = true)
    List<PedidoDto> hoy(@RequestParam(defaultValue = "false") boolean todos) {
        LocalDate hoy = LocalDate.now(reloj);
        List<Pedido> lista = todos ? pedidos.findByFechaOrderByCreadoEn(hoy)
                : pedidos.findByFechaAndEstadoInOrderByCreadoEn(hoy, EN_CURSO);
        return lista.stream().map(PedidoDto::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MOZO','CAJA')")
    @Transactional
    PedidoDto pedirPorMozo(@Valid @RequestBody PedidoMozoIn in) {
        return PedidoDto.de(servicio.pedirPorMozo(in.mesaId(), in.lineas().stream().map(LineaIn::aLinea).toList(), in.nota()));
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN','MOZO','CAJA')")
    @Transactional
    PedidoDto confirmar(@PathVariable Long id) {
        return PedidoDto.de(servicio.confirmar(id));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMIN','MOZO','CAJA')")
    @Transactional
    PedidoDto anular(@PathVariable Long id) {
        return PedidoDto.de(servicio.anular(id));
    }

    @PatchMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','COCINA','MOZO')")
    @Transactional
    PedidoDto item(@PathVariable Long itemId, @Valid @RequestBody ItemEstadoIn in) {
        return PedidoDto.de(servicio.avanzarItem(itemId, in.estado()));
    }

    @PostMapping("/{id}/estaciones/{estacionId}/listo")
    @PreAuthorize("hasAnyRole('ADMIN','COCINA')")
    @Transactional
    PedidoDto estacionLista(@PathVariable Long id, @PathVariable Long estacionId) {
        return PedidoDto.de(servicio.estacionLista(id, estacionId));
    }

    @PostMapping("/{id}/servir")
    @PreAuthorize("hasAnyRole('ADMIN','MOZO')")
    @Transactional
    PedidoDto servir(@PathVariable Long id) {
        return PedidoDto.de(servicio.servirListos(id));
    }

    @PostMapping("/{id}/despachar")
    @PreAuthorize("hasAnyRole('ADMIN','CAJA','MOZO')")
    @Transactional
    PedidoDto despachar(@PathVariable Long id) {
        return PedidoDto.de(servicio.despachar(id));
    }

    @PostMapping("/{id}/entregar")
    @PreAuthorize("hasAnyRole('ADMIN','CAJA','MOZO')")
    @Transactional
    PedidoDto entregar(@PathVariable Long id) {
        return PedidoDto.de(servicio.entregar(id));
    }
}
