package pe.dvega.restaurant.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cierre_caja")
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;

    @Column(name = "total_efectivo")
    private BigDecimal totalEfectivo;

    @Column(name = "total_yape")
    private BigDecimal totalYape;

    @Column(name = "total_plin")
    private BigDecimal totalPlin;

    @Column(name = "total_tarjeta")
    private BigDecimal totalTarjeta;

    @Column(name = "efectivo_contado")
    private BigDecimal efectivoContado;

    /** Contado menos esperado: negativo es faltante, positivo es sobrante. */
    private BigDecimal diferencia;

    private int tickets;

    @Column(name = "cerrado_por")
    private String cerradoPor;

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEn;

    protected CierreCaja() {
    }

    public CierreCaja(LocalDate fecha, BigDecimal efectivo, BigDecimal yape, BigDecimal plin, BigDecimal tarjeta,
                      BigDecimal efectivoContado, int tickets, String cerradoPor, LocalDateTime ahora) {
        this.fecha = fecha;
        this.totalEfectivo = efectivo;
        this.totalYape = yape;
        this.totalPlin = plin;
        this.totalTarjeta = tarjeta;
        this.efectivoContado = efectivoContado;
        this.diferencia = efectivoContado.subtract(efectivo);
        this.tickets = tickets;
        this.cerradoPor = cerradoPor;
        this.cerradoEn = ahora;
    }

    public BigDecimal total() {
        return totalEfectivo.add(totalYape).add(totalPlin).add(totalTarjeta);
    }

    public Long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public BigDecimal getTotalEfectivo() { return totalEfectivo; }
    public BigDecimal getTotalYape() { return totalYape; }
    public BigDecimal getTotalPlin() { return totalPlin; }
    public BigDecimal getTotalTarjeta() { return totalTarjeta; }
    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public BigDecimal getDiferencia() { return diferencia; }
    public int getTickets() { return tickets; }
    public String getCerradoPor() { return cerradoPor; }
    public LocalDateTime getCerradoEn() { return cerradoEn; }
}
