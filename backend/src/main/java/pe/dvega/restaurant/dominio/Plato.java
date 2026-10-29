package pe.dvega.restaurant.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class Plato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Categoria categoria;

    @ManyToOne(optional = false)
    private Estacion estacion;

    private String nombre;
    private String descripcion;
    private BigDecimal precio;

    /** Ruta pública de la foto (por ejemplo "platos/lomo-saltado.webp"); puede no tener. */
    private String foto;

    /** false = agotado por hoy. Se cambia al instante desde cocina, mozo o caja. */
    private boolean disponible = true;

    /** false = retirado de la carta. */
    private boolean activo = true;

    private int orden;

    protected Plato() {
    }

    public Plato(Categoria categoria, Estacion estacion, String nombre, String descripcion,
                 BigDecimal precio, String foto, int orden) {
        this.categoria = categoria;
        this.estacion = estacion;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.foto = foto;
        this.orden = orden;
    }

    public boolean sePuedePedir() {
        return activo && disponible;
    }

    public Long getId() { return id; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public Estacion getEstacion() { return estacion; }
    public void setEstacion(Estacion estacion) { this.estacion = estacion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
