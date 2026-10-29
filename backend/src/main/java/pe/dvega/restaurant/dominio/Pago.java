package pe.dvega.restaurant.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Pago {

    public enum Medio { EFECTIVO, YAPE, PLIN, TARJETA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Atencion atencion;

    @ManyToOne
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    private Medio medio;

    private BigDecimal monto;

    /** Solo en efectivo: lo que entregó el cliente y el vuelto. */
    private BigDecimal recibido;
    private BigDecimal vuelto;

    @Column(name = "cobrado_por")
    private String cobradoPor;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    protected Pago() {
    }

    public Pago(Atencion atencion, Pedido pedido, Medio medio, BigDecimal monto, BigDecimal recibido,
                String cobradoPor, LocalDateTime ahora) {
        this.atencion = atencion;
        this.pedido = pedido;
        this.medio = medio;
        this.monto = monto;
        if (medio == Medio.EFECTIVO && recibido != null) {
            if (recibido.compareTo(monto) < 0) {
                throw new ReglaNegocioException("El efectivo recibido no alcanza para el monto.");
            }
            this.recibido = recibido;
            this.vuelto = recibido.subtract(monto);
        }
        this.cobradoPor = cobradoPor;
        this.creadoEn = ahora;
    }

    public Long getId() { return id; }
    public Atencion getAtencion() { return atencion; }
    public Pedido getPedido() { return pedido; }
    public Medio getMedio() { return medio; }
    public BigDecimal getMonto() { return monto; }
    public BigDecimal getRecibido() { return recibido; }
    public BigDecimal getVuelto() { return vuelto; }
    public String getCobradoPor() { return cobradoPor; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
