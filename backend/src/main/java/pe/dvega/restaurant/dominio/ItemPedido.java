package pe.dvega.restaurant.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un plato dentro de un pedido. Guarda el nombre y el precio del momento: si la carta cambia, la cuenta no. */
@Entity
@Table(name = "item_pedido")
public class ItemPedido {

    public enum Estado { PENDIENTE, EN_PREPARACION, LISTO, SERVIDO, ANULADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Plato plato;

    @ManyToOne(optional = false)
    private Estacion estacion;

    private String nombre;

    @Column(name = "precio_unit")
    private BigDecimal precioUnit;

    private int cantidad;
    private String nota;

    @Enumerated(EnumType.STRING)
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    protected ItemPedido() {
    }

    ItemPedido(Pedido pedido, Plato plato, int cantidad, String nota, LocalDateTime ahora) {
        this.pedido = pedido;
        this.plato = plato;
        this.estacion = plato.getEstacion();
        this.nombre = plato.getNombre();
        this.precioUnit = plato.getPrecio();
        this.cantidad = cantidad;
        this.nota = nota;
        this.actualizadoEn = ahora;
    }

    public BigDecimal subtotal() {
        return precioUnit.multiply(BigDecimal.valueOf(cantidad));
    }

    /** La cocina avanza: pendiente → en preparación → listo; el mozo cierra con servido. */
    public void avanzar(Estado destino, LocalDateTime ahora) {
        boolean valido = switch (estado) {
            case PENDIENTE -> destino == Estado.EN_PREPARACION || destino == Estado.LISTO;
            case EN_PREPARACION -> destino == Estado.LISTO || destino == Estado.PENDIENTE;
            case LISTO -> destino == Estado.SERVIDO || destino == Estado.EN_PREPARACION;
            case SERVIDO, ANULADO -> false;
        };
        if (!valido) {
            throw new ReglaNegocioException("«" + nombre + "» no puede pasar de " + estado + " a " + destino + ".");
        }
        estado = destino;
        actualizadoEn = ahora;
    }

    void anular(LocalDateTime ahora) {
        if (estado != Estado.SERVIDO) {
            estado = Estado.ANULADO;
            actualizadoEn = ahora;
        }
    }

    /** Solo para datos de demostración. */
    public void forzarEstado(Estado estado, LocalDateTime cuando) {
        this.estado = estado;
        this.actualizadoEn = cuando;
    }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public Plato getPlato() { return plato; }
    public Estacion getEstacion() { return estacion; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecioUnit() { return precioUnit; }
    public int getCantidad() { return cantidad; }
    public String getNota() { return nota; }
    public Estado getEstado() { return estado; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
