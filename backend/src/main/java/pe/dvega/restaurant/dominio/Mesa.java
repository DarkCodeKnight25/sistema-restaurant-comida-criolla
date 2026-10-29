package pe.dvega.restaurant.dominio;

import jakarta.persistence.*;

@Entity
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int numero;

    /** Va en el QR de la mesa; no es adivinable, así nadie pide a nombre de otra mesa. */
    private String token;

    private int capacidad;
    private String zona;

    protected Mesa() {
    }

    public Mesa(int numero, String token, int capacidad, String zona) {
        this.numero = numero;
        this.token = token;
        this.capacidad = capacidad;
        this.zona = zona;
    }

    public Long getId() { return id; }
    public int getNumero() { return numero; }
    public String getToken() { return token; }
    public int getCapacidad() { return capacidad; }
    public String getZona() { return zona; }
}
