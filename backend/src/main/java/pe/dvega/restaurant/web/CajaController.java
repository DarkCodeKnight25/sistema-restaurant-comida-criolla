package pe.dvega.restaurant.web;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.dvega.restaurant.dominio.*;
import pe.dvega.restaurant.repositorio.*;
import pe.dvega.restaurant.servicio.CajaService;
import pe.dvega.restaurant.web.Dtos.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Mapa de mesas, cuentas, cobros y cierre de caja. */
@RestController
@RequestMapping("/api")
public class CajaController {

    private final CajaService caja;
    private final MesaRepositorio mesas;
    private final AtencionRepositorio atenciones;
    private final PedidoRepositorio pedidos;
    private final CierreCajaRepositorio cierres;
    private final Clock reloj;

    public CajaController(CajaService caja, MesaRepositorio mesas, AtencionRepositorio atenciones,
                          PedidoRepositorio pedidos, CierreCajaRepositorio cierres, Clock reloj) {
        this.caja = caja;
        this.mesas = mesas;
        this.atenciones = atenciones;
        this.pedidos = pedidos;
        this.cierres = cierres;
        this.reloj = reloj;
    }

    @GetMapping("/mesas")
    @Transactional(readOnly = true)
    List<MesaDto> mesas() {
        Map<Long, Atencion> activas = atenciones.activas().stream()
                .collect(java.util.stream.Collectors.toMap(a -> a.getMesa().getId(), a -> a));
        return mesas.findAllByOrderByNumero().stream().map(m -> {
            Atencion a = activas.get(m.getId());
            AtencionDto dto = null;
            if (a != null) {
                List<Pedido> lista = pedidos.findByAtencionIdOrderByCreadoEn(a.getId());
                BigDecimal consumo = lista.stream().filter(p -> p.getEstado() != Pedido.Estado.ANULADO)
                        .map(Pedido::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                int porConfirmar = (int) lista.stream().filter(p -> p.getEstado() == Pedido.Estado.ENVIADO).count();
                int listos = (int) lista.stream().flatMap(p -> p.getItems().stream())
                        .filter(i -> i.getEstado() == ItemPedido.Estado.LISTO).count();
                dto = new AtencionDto(a.getId(), a.getEstado(), a.getAbiertaEn(), a.getCuentaEn(), consumo, porConfirmar, listos);
            }
            return new MesaDto(m.getId(), m.getNumero(), m.getZona(), m.getCapacidad(), m.getToken(), dto);
        }).toList();
    }

    @GetMapping("/caja/cuentas/{atencionId}")
    @Transactional(readOnly = true)
    CuentaDto cuenta(@PathVariable Long atencionId) {
        CajaService.Cuenta c = caja.cuenta(atencionId);
        return new CuentaDto(c.atencion().getId(), c.atencion().getMesa().getNumero(), c.atencion().getEstado(),
                c.pedidos().stream().map(PedidoDto::de).toList(), c.total(), c.pagado(), c.saldo());
    }

    @PostMapping("/caja/cuentas/{atencionId}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','CAJA')")
    PagoDto cobrarMesa(@PathVariable Long atencionId, @Valid @RequestBody CobroMesaIn in, @AuthenticationPrincipal Jwt jwt) {
        return PagoDto.de(caja.cobrarMesa(atencionId, in.medio(), in.monto(), in.recibido(), jwt.getClaimAsString("nombre")));
    }

    @PostMapping("/caja/pedidos/{pedidoId}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','CAJA')")
    PagoDto cobrarPedido(@PathVariable Long pedidoId, @Valid @RequestBody CobroPedidoIn in, @AuthenticationPrincipal Jwt jwt) {
        return PagoDto.de(caja.cobrarPedido(pedidoId, in.medio(), in.recibido(), jwt.getClaimAsString("nombre")));
    }

    @GetMapping("/caja/resumen")
    @PreAuthorize("hasAnyRole('ADMIN','CAJA')")
    CajaService.Resumen resumen(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return caja.resumen(fecha != null ? fecha : LocalDate.now(reloj));
    }

    @PostMapping("/caja/cierres")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','CAJA')")
    CierreDto cerrar(@Valid @RequestBody CierreIn in, @AuthenticationPrincipal Jwt jwt) {
        return CierreDto.de(caja.cerrar(in.fecha(), in.efectivoContado(), jwt.getClaimAsString("nombre")));
    }

    @GetMapping("/caja/cierres")
    @PreAuthorize("hasAnyRole('ADMIN','CAJA')")
    List<CierreDto> cierres() {
        return cierres.findTop14ByOrderByFechaDesc().stream()
                .sorted(Comparator.comparing(CierreCaja::getFecha)).map(CierreDto::de).toList();
    }
}
