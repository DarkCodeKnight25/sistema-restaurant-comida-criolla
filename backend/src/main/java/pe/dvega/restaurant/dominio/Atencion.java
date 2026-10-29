package pe.dvega.restaurant.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** La visita de un grupo a una mesa: se abre con el primer pedido y se cierra al pagar. */
@Entity
public class Atencion {

    public enum Estado { ABIERTA, CUENTA, PAGADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Mesa mesa;

    @Enumerated(EnumType.STRING)
    private Estado estado = Estado.ABIERTA;

    @Column(name = "abierta_en")
    private LocalDateTime abiertaEn;

    @Column(name = "cuenta_en")
    private LocalDateTime cuentaEn;

    @Column(name = "cerrada_en")
    private LocalDateTime cerradaEn;

    protected Atencion() {
    }

    public Atencion(Mesa mesa, LocalDateTime ahora) {
        this.mesa = mesa;
        this.abiertaEn = ahora;
    }

    public void pedirCuenta(LocalDateTime ahora) {
        if (estado == Estado.PAGADA) {
            throw new ReglaNegocioException("Esta mesa ya pagó.");
        }
        estado = Estado.CUENTA;
        cuentaEn = ahora;
    }

    /** Si el grupo pide algo más después de pedir la cuenta, la mesa vuelve a estar abierta. */
    public void reabrir() {
        if (estado == Estado.CUENTA) {
            estado = Estado.ABIERTA;
            cuentaEn = null;
        }
    }

    public void cerrar(LocalDateTime ahora) {
        estado = Estado.PAGADA;
        cerradaEn = ahora;
    }

    public boolean activa() {
        return estado != Estado.PAGADA;
    }

    public Long getId() { return id; }
    public Mesa getMesa() { return mesa; }
    public Estado getEstado() { return estado; }
    public LocalDateTime getAbiertaEn() { return abiertaEn; }
    public LocalDateTime getCuentaEn() { return cuentaEn; }
    public LocalDateTime getCerradaEn() { return cerradaEn; }
}
