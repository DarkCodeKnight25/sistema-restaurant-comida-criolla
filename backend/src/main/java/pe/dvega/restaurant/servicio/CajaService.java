package pe.dvega.restaurant.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.dvega.restaurant.dominio.*;
import pe.dvega.restaurant.repositorio.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Cobros por mesa o por pedido, y cierre del día. Todo se suma desde pedidos y pagos guardados. */
@Service
public class CajaService {

    private final AtencionRepositorio atenciones;
    private final PedidoRepositorio pedidos;
    private final PagoRepositorio pagos;
    private final CierreCajaRepositorio cierres;
    private final Eventos eventos;
    private final Clock reloj;

    public CajaService(AtencionRepositorio atenciones, PedidoRepositorio pedidos, PagoRepositorio pagos,
                       CierreCajaRepositorio cierres, Eventos eventos, Clock reloj) {
        this.atenciones = atenciones;
        this.pedidos = pedidos;
        this.pagos = pagos;
        this.cierres = cierres;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    public record Cuenta(Atencion atencion, List<Pedido> pedidos, BigDecimal total, BigDecimal pagado, BigDecimal saldo) {
    }

    @Transactional(readOnly = true)
    public Cuenta cuenta(Long atencionId) {
        Atencion a = atenciones.findById(atencionId).orElseThrow(() -> new NoEncontradoException("No existe esa cuenta."));
        return cuentaDe(a);
    }

    private Cuenta cuentaDe(Atencion a) {
        List<Pedido> lista = pedidos.findByAtencionIdOrderByCreadoEn(a.getId()).stream()
                .filter(p -> p.getEstado() != Pedido.Estado.ANULADO && p.getEstado() != Pedido.Estado.ENVIADO).toList();
        BigDecimal total = lista.stream().map(Pedido::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pagado = pagos.pagadoEnAtencion(a.getId());
        return new Cuenta(a, lista, total, pagado, total.subtract(pagado).max(BigDecimal.ZERO));
    }

    /** Cobra una mesa; se puede pagar en partes. Cuando el saldo llega a cero, la mesa queda libre. */
    @Transactional
    public Pago cobrarMesa(Long atencionId, Pago.Medio medio, BigDecimal monto, BigDecimal recibido, String usuario) {
        Atencion a = atenciones.bloquear(atencionId).orElseThrow(() -> new NoEncontradoException("No existe esa cuenta."));
        if (!a.activa()) {
            throw new ReglaNegocioException("Esta mesa ya pagó.");
        }
        Cuenta c = cuentaDe(a);
        boolean porConfirmar = pedidos.findByAtencionIdOrderByCreadoEn(a.getId()).stream()
                .anyMatch(p -> p.getEstado() == Pedido.Estado.ENVIADO);
        if (porConfirmar) {
            throw new ReglaNegocioException("La mesa tiene un pedido sin confirmar. Confírmalo o anúlalo antes de cobrar.");
        }
        validarMonto(monto, c.saldo());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Pago pago = pagos.save(new Pago(a, null, medio, monto, recibido, usuario, ahora));
        if (c.saldo().subtract(monto).signum() == 0) {
            a.cerrar(ahora);
            c.pedidos().forEach(Pedido::marcarPagado);
        }
        eventos.avisar(Eventos.Tipo.PAGO, a.getId(), a.getMesa().getToken(), null);
        return pago;
    }

    /** Cobra un pedido para llevar o delivery, completo. */
    @Transactional
    public Pago cobrarPedido(Long pedidoId, Pago.Medio medio, BigDecimal recibido, String usuario) {
        pedidos.bloquear(pedidoId).orElseThrow(() -> new NoEncontradoException("No existe el pedido."));
        Pedido p = pedidos.detalle(pedidoId).orElseThrow();
        if (p.getTipo() == Pedido.Tipo.MESA) {
            throw new ReglaNegocioException("Los pedidos de mesa se cobran en la cuenta de la mesa.");
        }
        if (p.isPagado() || p.getEstado() == Pedido.Estado.ANULADO) {
            throw new ReglaNegocioException("El pedido #" + p.getNumero() + " ya está pagado o anulado.");
        }
        Pago pago = pagos.save(new Pago(null, p, medio, p.getTotal(), recibido, usuario, LocalDateTime.now(reloj)));
        p.marcarPagado();
        eventos.avisar(Eventos.Tipo.PAGO, p.getId(), null, p.getCodigo());
        return pago;
    }

    private static void validarMonto(BigDecimal monto, BigDecimal saldo) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("El monto debe ser mayor a cero.");
        }
        if (monto.compareTo(saldo) > 0) {
            throw new ReglaNegocioException("El monto (S/ " + monto + ") es mayor que el saldo (S/ " + saldo + ").");
        }
    }

    // ---------- Resumen y cierre ----------

    public record PlatoVendido(String nombre, int cantidad, BigDecimal importe) {
    }

    public record Resumen(LocalDate fecha, Map<Pago.Medio, BigDecimal> porMedio, BigDecimal total, int tickets,
                          BigDecimal ticketPromedio, int[] ventasPorHora, List<PlatoVendido> masVendidos,
                          int mesasAbiertas, int externosSinCobrar, CierreCaja cierre) {
    }

    @Transactional(readOnly = true)
    public Resumen resumen(LocalDate fecha) {
        List<Pago> delDia = pagos.findByCreadoEnBetweenOrderByCreadoEn(fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        Map<Pago.Medio, BigDecimal> porMedio = new EnumMap<>(Pago.Medio.class);
        for (Pago.Medio m : Pago.Medio.values()) {
            porMedio.put(m, BigDecimal.ZERO);
        }
        int[] porHora = new int[24];
        for (Pago p : delDia) {
            porMedio.merge(p.getMedio(), p.getMonto(), BigDecimal::add);
            porHora[p.getCreadoEn().getHour()] += p.getMonto().intValue();
        }
        BigDecimal total = porMedio.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        // Un ticket es una mesa cobrada o un pedido externo cobrado.
        Set<String> tickets = delDia.stream()
                .map(p -> p.getAtencion() != null ? "A" + p.getAtencion().getId() : "P" + p.getPedido().getId())
                .collect(Collectors.toSet());
        BigDecimal promedio = tickets.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(tickets.size()), 2, java.math.RoundingMode.HALF_UP);

        List<Pedido> pedidosDia = pedidos.findByFechaOrderByCreadoEn(fecha);
        Map<String, int[]> cantidades = new HashMap<>();
        Map<String, BigDecimal> importes = new HashMap<>();
        pedidosDia.stream().filter(p -> p.getEstado() != Pedido.Estado.ANULADO).flatMap(p -> p.getItems().stream())
                .filter(i -> i.getEstado() != ItemPedido.Estado.ANULADO)
                .forEach(i -> {
                    cantidades.computeIfAbsent(i.getNombre(), k -> new int[1])[0] += i.getCantidad();
                    importes.merge(i.getNombre(), i.subtotal(), BigDecimal::add);
                });
        List<PlatoVendido> top = cantidades.entrySet().stream()
                .map(e -> new PlatoVendido(e.getKey(), e.getValue()[0], importes.get(e.getKey())))
                .sorted(Comparator.comparingInt(PlatoVendido::cantidad).reversed().thenComparing(PlatoVendido::nombre))
                .limit(5).toList();

        int abiertas = fecha.equals(LocalDate.now(reloj)) ? atenciones.activas().size() : 0;
        int externos = (int) pedidosDia.stream()
                .filter(p -> p.getTipo() != Pedido.Tipo.MESA && !p.isPagado() && p.getEstado() != Pedido.Estado.ANULADO).count();
        return new Resumen(fecha, porMedio, total, tickets.size(), promedio, porHora, top, abiertas, externos,
                cierres.findByFecha(fecha).orElse(null));
    }

    @Transactional
    public CierreCaja cerrar(LocalDate fecha, BigDecimal efectivoContado, String usuario) {
        if (cierres.findByFecha(fecha).isPresent()) {
            throw new ReglaNegocioException("La caja del " + fecha + " ya está cerrada.");
        }
        Resumen r = resumen(fecha);
        if (r.mesasAbiertas() > 0) {
            throw new ReglaNegocioException("Hay " + r.mesasAbiertas() + " mesas sin cobrar. Cóbralas antes de cerrar la caja.");
        }
        if (r.externosSinCobrar() > 0) {
            throw new ReglaNegocioException("Hay " + r.externosSinCobrar() + " pedidos para llevar o delivery sin cobrar.");
        }
        CierreCaja c = cierres.save(new CierreCaja(fecha, r.porMedio().get(Pago.Medio.EFECTIVO), r.porMedio().get(Pago.Medio.YAPE),
                r.porMedio().get(Pago.Medio.PLIN), r.porMedio().get(Pago.Medio.TARJETA), efectivoContado, r.tickets(), usuario,
                LocalDateTime.now(reloj)));
        eventos.avisar(Eventos.Tipo.PAGO, c.getId(), null, null);
        return c;
    }
}
