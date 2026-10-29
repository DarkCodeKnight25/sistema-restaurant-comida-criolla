package pe.dvega.restaurant.dominio;

import jakarta.persistence.*;

@Entity
public class Usuario {

    public enum Rol { ADMIN, MOZO, COCINA, CAJA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String nombre;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    protected Usuario() {
    }

    public Usuario(String email, String nombre, String passwordHash, Rol rol) {
        this.email = email;
        this.nombre = nombre;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
}
