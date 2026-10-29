package pe.dvega.restaurant.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "contador_dia")
public class ContadorDia {

    @Id
    private LocalDate fecha;

    private int ultimo;

    protected ContadorDia() {
    }

    public ContadorDia(LocalDate fecha) {
        this.fecha = fecha;
    }

    /** Solo para datos de demostración: el día empieza con pedidos ya numerados. */
    public ContadorDia(LocalDate fecha, int ultimo) {
        this.fecha = fecha;
        this.ultimo = ultimo;
    }

    public int siguiente() {
        return ++ultimo;
    }

    public LocalDate getFecha() { return fecha; }
    public int getUltimo() { return ultimo; }
}
