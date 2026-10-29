package pe.dvega.restaurant.dominio;

import jakarta.persistence.*;

/** Estación de cocina (fríos, calientes, bar...). Cada restaurante define las suyas. */
@Entity
public class Estacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;
    private String nombre;

    /** Color de la etiqueta de caja de la estación, igual en carta, cocina y caja. */
    private String color;

    private int orden;

    protected Estacion() {
    }

    public Estacion(String codigo, String nombre, String color, int orden) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.color = color;
        this.orden = orden;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getColor() { return color; }
    public int getOrden() { return orden; }
}
