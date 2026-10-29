package pe.dvega.restaurant.dominio;

import jakarta.persistence.*;

@Entity
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private int orden;

    protected Categoria() {
    }

    public Categoria(String nombre, int orden) {
        this.nombre = nombre;
        this.orden = orden;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getOrden() { return orden; }
}
