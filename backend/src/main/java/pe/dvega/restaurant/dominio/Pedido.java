package pe.dvega.restaurant.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Pedido {

    public enum Tipo { MESA, LLEVAR, DELIVERY }

    /**
     * ENVIADO: llegó y espera que el mozo o la caja lo confirme. Desde CONFIRMADO la cocina lo ve.
     * EN_PREPARACION, LISTO y SERVIDO se calculan de sus platos. Para llevar y delivery: LISTO, EN_CAMINO, ENTREGADO.
     */
    public enum Estado { ENVIADO, CONFIRMADO, EN_PREPARACION, LISTO, SERVIDO, EN_CAMINO, ENTREGADO, ANULADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código para seguir un pedido para llevar o delivery desde el celular. */
    private String codigo;

    private LocalDate fecha;

    /** Número correlativo del día; es el titular en la pantalla de cocina (#047). */
    private int numero;

    @Enumerated(EnumType.STRING)
    private Tipo tipo;

    @ManyToOne
    private Atencion atencion;

    @Column(name = "cliente_nombre")
    private String clienteNombre;

    @Column(name = "cliente_celular")
    private String clienteCelular;

    private String direccion;
    private String referencia;

    @Enumerated(EnumType.STRING)
    private Estado estado = Estado.ENVIADO;

    private String nota;
    private BigDecimal total = BigDecimal.ZERO;
    private boolean pagado;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemPedido> items = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(String codigo, LocalDate fecha, int numero, Tipo tipo, Atencion atencion, LocalDateTime ahora) {
        this.codigo = codigo;
        this.fecha = fecha;
        this.numero = numero;
        this.tipo = tipo;
        this.atencion = atencion;
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    public void agregar(Plato plato, int cantidad, String nota, LocalDateTime ahora) {
        items.add(new ItemPedido(this, plato, cantidad, nota, ahora));
        recalcularTotal();
    }

    public void recalcularTotal() {
        total = items.stream()
                .filter(i -> i.getEstado() != ItemPedido.Estado.ANULADO)
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void confirmar(LocalDateTime ahora) {
        if (estado != Estado.ENVIADO) {
            throw new ReglaNegocioException("El pedido #" + numero + " ya fue confirmado.");
        }
        estado = Estado.CONFIRMADO;
        actualizadoEn = ahora;
    }

    public void anular(LocalDateTime ahora) {
        if (pagado || estado == Estado.SERVIDO || estado == Estado.ENTREGADO) {
            throw new ReglaNegocioException("No se puede anular un pedido ya servido o pagado.");
        }
        estado = Estado.ANULADO;
        items.forEach(i -> i.anular(ahora));
        recalcularTotal();
        actualizadoEn = ahora;
    }

    /** El estado del pedido sigue al de sus platos una vez que la cocina lo tiene. */
    public void sincronizarConItems(LocalDateTime ahora) {
        if (estado == Estado.ENVIADO || estado == Estado.ANULADO || estado == Estado.EN_CAMINO || estado == Estado.ENTREGADO) {
            return;
        }
        List<ItemPedido> vivos = items.stream().filter(i -> i.getEstado() != ItemPedido.Estado.ANULADO).toList();
        if (vivos.isEmpty()) {
            return;
        }
        Estado nuevo;
        if (vivos.stream().allMatch(i -> i.getEstado() == ItemPedido.Estado.SERVIDO)) {
            nuevo = tipo == Tipo.MESA ? Estado.SERVIDO : Estado.LISTO;
        } else if (vivos.stream().allMatch(i -> i.getEstado() == ItemPedido.Estado.LISTO || i.getEstado() == ItemPedido.Estado.SERVIDO)) {
            nuevo = Estado.LISTO;
        } else if (vivos.stream().anyMatch(i -> i.getEstado() != ItemPedido.Estado.PENDIENTE)) {
            nuevo = Estado.EN_PREPARACION;
        } else {
            nuevo = Estado.CONFIRMADO;
        }
        if (nuevo != estado) {
            estado = nuevo;
            actualizadoEn = ahora;
        }
    }

    public void despachar(LocalDateTime ahora) {
        if (tipo != Tipo.DELIVERY || estado != Estado.LISTO) {
            throw new ReglaNegocioException("Solo un delivery listo puede salir a reparto.");
        }
        estado = Estado.EN_CAMINO;
        actualizadoEn = ahora;
    }

    public void entregar(LocalDateTime ahora) {
        boolean puede = (tipo == Tipo.LLEVAR && estado == Estado.LISTO) || (tipo == Tipo.DELIVERY && estado == Estado.EN_CAMINO);
        if (!puede) {
            throw new ReglaNegocioException("El pedido #" + numero + " todavía no está listo para entregar.");
        }
        estado = Estado.ENTREGADO;
        actualizadoEn = ahora;
    }

    public void marcarPagado() {
        this.pagado = true;
    }

    public void datosCliente(String nombre, String celular, String direccion, String referencia) {
        this.clienteNombre = nombre;
        this.clienteCelular = celular;
        this.direccion = direccion;
        this.referencia = referencia;
    }

    /** Solo para datos de demostración. */
    public void forzarEstado(Estado estado, LocalDateTime cuando) {
        this.estado = estado;
        this.actualizadoEn = cuando;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public LocalDate getFecha() { return fecha; }
    public int getNumero() { return numero; }
    public Tipo getTipo() { return tipo; }
    public Atencion getAtencion() { return atencion; }
    public String getClienteNombre() { return clienteNombre; }
    public String getClienteCelular() { return clienteCelular; }
    public String getDireccion() { return direccion; }
    public String getReferencia() { return referencia; }
    public Estado getEstado() { return estado; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }
    public BigDecimal getTotal() { return total; }
    public boolean isPagado() { return pagado; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public List<ItemPedido> getItems() { return items; }
}
