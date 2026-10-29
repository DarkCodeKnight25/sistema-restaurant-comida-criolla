package pe.dvega.restaurant.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.dvega.restaurant.dominio.*;
import pe.dvega.restaurant.repositorio.*;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/** Pedidos de mesa, para llevar y delivery, y su paso por la cocina. */
@Service
public class PedidoService {

    private static final char[] ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    static final int MAX_LINEAS = 30;

    private final PedidoRepositorio pedidos;
    private final PlatoRepositorio platos;
    private final MesaRepositorio mesas;
    private final AtencionRepositorio atenciones;
    private final ItemPedidoRepositorio items;
    private final Numerador numerador;
    private final Eventos eventos;
    private final Clock reloj;
    private final SecureRandom azar = new SecureRandom();

    public PedidoService(PedidoRepositorio pedidos, PlatoRepositorio platos, MesaRepositorio mesas,
                         AtencionRepositorio atenciones, ItemPedidoRepositorio items, Numerador numerador,
                         Eventos eventos, Clock reloj) {
        this.pedidos = pedidos;
        this.platos = platos;
        this.mesas = mesas;
        this.atenciones = atenciones;
        this.items = items;
        this.numerador = numerador;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    public record Linea(Long platoId, int cantidad, String nota) {
    }

    public record DatosCliente(String nombre, String celular, String direccion, String referencia) {
    }

    // ---------- Crear ----------

    /** El comensal pide desde el QR de su mesa. La primera vez abre la atención de la mesa. */
    @Transactional
    public Pedido pedirEnMesa(String token, List<Linea> lineas, String nota) {
        Mesa mesa = mesas.findByToken(token).orElseThrow(() -> new NoEncontradoException("Este QR no corresponde a ninguna mesa."));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Atencion atencion = atenciones.activa(mesa.getId()).orElseGet(() -> atenciones.save(new Atencion(mesa, ahora)));
        atencion.reabrir();
        Pedido p = nuevo(Pedido.Tipo.MESA, atencion, lineas, nota, ahora);
        eventos.avisar(Eventos.Tipo.PEDIDO, p.getId(), mesa.getToken(), null);
        return p;
    }

    /** El mozo registra el pedido de una mesa desde su celular: entra ya confirmado. */
    @Transactional
    public Pedido pedirPorMozo(Long mesaId, List<Linea> lineas, String nota) {
        Mesa mesa = mesas.findById(mesaId).orElseThrow(() -> new NoEncontradoException("No existe la mesa."));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Atencion atencion = atenciones.activa(mesa.getId()).orElseGet(() -> atenciones.save(new Atencion(mesa, ahora)));
        atencion.reabrir();
        Pedido p = nuevo(Pedido.Tipo.MESA, atencion, lineas, nota, ahora);
        p.confirmar(ahora);
        eventos.avisar(Eventos.Tipo.PEDIDO, p.getId(), mesa.getToken(), null);
        return p;
    }

    /** Para llevar o delivery desde la carta pública: necesita nombre y celular, y dirección si es delivery. */
    @Transactional
    public Pedido pedirExterno(Pedido.Tipo tipo, DatosCliente cliente, List<Linea> lineas, String nota) {
        if (tipo == Pedido.Tipo.MESA) {
            throw new ReglaNegocioException("Un pedido de mesa se hace desde el QR de la mesa.");
        }
        if (tipo == Pedido.Tipo.DELIVERY && (cliente.direccion() == null || cliente.direccion().isBlank())) {
            throw new ReglaNegocioException("Para delivery necesitamos la dirección.");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Pedido p = nuevo(tipo, null, lineas, nota, ahora);
        p.datosCliente(cliente.nombre().trim(), cliente.celular(), blancoANulo(cliente.direccion()), blancoANulo(cliente.referencia()));
        eventos.avisar(Eventos.Tipo.PEDIDO, p.getId(), null, p.getCodigo());
        return p;
    }

    private Pedido nuevo(Pedido.Tipo tipo, Atencion atencion, List<Linea> lineas, String nota, LocalDateTime ahora) {
        if (lineas == null || lineas.isEmpty()) {
            throw new ReglaNegocioException("El pedido está vacío.");
        }
        if (lineas.size() > MAX_LINEAS) {
            throw new ReglaNegocioException("Un pedido tiene como máximo " + MAX_LINEAS + " líneas.");
        }
        Map<Long, Plato> porId = new HashMap<>();
        platos.findAllById(lineas.stream().map(Linea::platoId).toList()).forEach(pl -> porId.put(pl.getId(), pl));

        List<Long> agotados = lineas.stream().map(Linea::platoId).distinct()
                .filter(id -> porId.get(id) == null || !porId.get(id).sePuedePedir()).toList();
        if (!agotados.isEmpty()) {
            String nombres = agotados.stream().map(id -> porId.containsKey(id) ? porId.get(id).getNombre() : "un plato")
                    .reduce((a, b) -> a + ", " + b).orElse("");
            throw new ConflictoException("Se acabó: " + nombres + ". Quítalo del pedido para enviarlo.", agotados);
        }

        int numero = numerador.siguiente(ahora.toLocalDate());
        Pedido p = new Pedido(nuevoCodigo(), ahora.toLocalDate(), numero, tipo, atencion, ahora);
        p.setNota(blancoANulo(nota));
        // El precio sale siempre de la carta guardada, nunca de lo que mande el navegador.
        lineas.forEach(l -> p.agregar(porId.get(l.platoId()), l.cantidad(), blancoANulo(l.nota()), ahora));
        return pedidos.save(p);
    }

    // ---------- Flujo ----------

    @Transactional
    public Pedido confirmar(Long pedidoId) {
        Pedido p = bloqueado(pedidoId);
        p.confirmar(LocalDateTime.now(reloj));
        avisar(p);
        return p;
    }

    @Transactional
    public Pedido anular(Long pedidoId) {
        Pedido p = bloqueado(pedidoId);
        p.anular(LocalDateTime.now(reloj));
        avisar(p);
        return p;
    }

    /** Cocina o mozo cambia el estado de un plato; el pedido se recalcula solo. */
    @Transactional
    public Pedido avanzarItem(Long itemId, ItemPedido.Estado destino) {
        ItemPedido item = items.findById(itemId).orElseThrow(() -> new NoEncontradoException("No existe ese plato del pedido."));
        Pedido p = bloqueado(item.getPedido().getId());
        if (p.getEstado() == Pedido.Estado.ENVIADO) {
            throw new ReglaNegocioException("Primero hay que confirmar el pedido #" + p.getNumero() + ".");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        item.avanzar(destino, ahora);
        p.sincronizarConItems(ahora);
        avisar(p);
        return p;
    }

    /** Toda una estación de un pedido pasa a listo de una vez (botón grande de la cocina). */
    @Transactional
    public Pedido estacionLista(Long pedidoId, Long estacionId) {
        Pedido p = bloqueado(pedidoId);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        p.getItems().stream()
                .filter(i -> i.getEstacion().getId().equals(estacionId))
                .filter(i -> i.getEstado() == ItemPedido.Estado.PENDIENTE || i.getEstado() == ItemPedido.Estado.EN_PREPARACION)
                .forEach(i -> i.avanzar(ItemPedido.Estado.LISTO, ahora));
        p.sincronizarConItems(ahora);
        avisar(p);
        return p;
    }

    /** El mozo lleva a la mesa todo lo que está listo. */
    @Transactional
    public Pedido servirListos(Long pedidoId) {
        Pedido p = bloqueado(pedidoId);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        p.getItems().stream().filter(i -> i.getEstado() == ItemPedido.Estado.LISTO)
                .forEach(i -> i.avanzar(ItemPedido.Estado.SERVIDO, ahora));
        p.sincronizarConItems(ahora);
        avisar(p);
        return p;
    }

    @Transactional
    public Pedido despachar(Long pedidoId) {
        Pedido p = bloqueado(pedidoId);
        p.despachar(LocalDateTime.now(reloj));
        avisar(p);
        return p;
    }

    @Transactional
    public Pedido entregar(Long pedidoId) {
        Pedido p = bloqueado(pedidoId);
        if (!p.isPagado()) {
            throw new ReglaNegocioException("Cobra el pedido #" + p.getNumero() + " antes de entregarlo.");
        }
        p.entregar(LocalDateTime.now(reloj));
        avisar(p);
        return p;
    }

    @Transactional
    public Atencion pedirCuenta(String token) {
        Mesa mesa = mesas.findByToken(token).orElseThrow(() -> new NoEncontradoException("Este QR no corresponde a ninguna mesa."));
        Atencion a = atenciones.activa(mesa.getId())
                .orElseThrow(() -> new ReglaNegocioException("Todavía no hay nada que cobrar en esta mesa."));
        a.pedirCuenta(LocalDateTime.now(reloj));
        eventos.avisar(Eventos.Tipo.MESA, a.getId(), mesa.getToken(), null);
        return a;
    }

    private void avisar(Pedido p) {
        String token = p.getAtencion() == null ? null : p.getAtencion().getMesa().getToken();
        eventos.avisar(Eventos.Tipo.PEDIDO, p.getId(), token, p.getTipo() == Pedido.Tipo.MESA ? null : p.getCodigo());
    }

    private Pedido bloqueado(Long id) {
        pedidos.bloquear(id).orElseThrow(() -> new NoEncontradoException("No existe el pedido."));
        return pedidos.detalle(id).orElseThrow();
    }

    private String nuevoCodigo() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder("DM-");
            for (int i = 0; i < 5; i++) {
                sb.append(ALFABETO[azar.nextInt(ALFABETO.length)]);
            }
            codigo = sb.toString();
        } while (pedidos.existsByCodigo(codigo));
        return codigo;
    }

    private static String blancoANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
