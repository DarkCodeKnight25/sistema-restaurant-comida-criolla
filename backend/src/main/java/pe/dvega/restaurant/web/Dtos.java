package pe.dvega.restaurant.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pe.dvega.restaurant.dominio.*;
import pe.dvega.restaurant.servicio.PedidoService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Contratos JSON. Fechas y horas en hora local de Lima, sin zona. */
public final class Dtos {

    private Dtos() {
    }

    // ---------- Carta ----------

    public record EstacionDto(Long id, String codigo, String nombre, String color) {
        static EstacionDto de(Estacion e) {
            return new EstacionDto(e.getId(), e.getCodigo(), e.getNombre(), e.getColor());
        }
    }

    public record CategoriaDto(Long id, String nombre) {
        static CategoriaDto de(Categoria c) {
            return new CategoriaDto(c.getId(), c.getNombre());
        }
    }

    public record PlatoDto(Long id, Long categoriaId, Long estacionId, String nombre, String descripcion,
                           BigDecimal precio, String foto, boolean disponible, boolean activo) {
        static PlatoDto de(Plato p) {
            return new PlatoDto(p.getId(), p.getCategoria().getId(), p.getEstacion().getId(), p.getNombre(),
                    p.getDescripcion(), p.getPrecio(), p.getFoto(), p.isDisponible(), p.isActivo());
        }
    }

    public record CartaDto(List<EstacionDto> estaciones, List<CategoriaDto> categorias, List<PlatoDto> platos) {
    }

    public record LocalDto(String nombre, String tipo, String direccion, String distrito, String horario,
                           String celular, String yapeDemo, boolean demo, LocalDateTime ahora) {
    }

    public record PlatoIn(@NotNull Long categoriaId, @NotNull Long estacionId,
                          @NotBlank @Size(max = 80) String nombre, @Size(max = 240) String descripcion,
                          @NotNull @DecimalMin("0.50") @Digits(integer = 6, fraction = 2) BigDecimal precio,
                          @Size(max = 120) String foto, boolean activo) {
    }

    public record DisponibleIn(boolean disponible) {
    }

    // ---------- Pedidos ----------

    public record ItemDto(Long id, Long platoId, Long estacionId, String nombre, int cantidad, BigDecimal precioUnit,
                          String nota, ItemPedido.Estado estado, LocalDateTime actualizadoEn) {
        static ItemDto de(ItemPedido i) {
            return new ItemDto(i.getId(), i.getPlato().getId(), i.getEstacion().getId(), i.getNombre(), i.getCantidad(),
                    i.getPrecioUnit(), i.getNota(), i.getEstado(), i.getActualizadoEn());
        }
    }

    public record PedidoDto(Long id, String codigo, int numero, Pedido.Tipo tipo, Pedido.Estado estado,
                            Integer mesa, Long atencionId, String clienteNombre, String clienteCelular, String direccion,
                            String referencia, String nota, BigDecimal total, boolean pagado,
                            LocalDateTime creadoEn, LocalDateTime actualizadoEn, List<ItemDto> items) {
        static PedidoDto de(Pedido p) {
            Atencion a = p.getAtencion();
            return new PedidoDto(p.getId(), p.getCodigo(), p.getNumero(), p.getTipo(), p.getEstado(),
                    a == null ? null : a.getMesa().getNumero(), a == null ? null : a.getId(),
                    p.getClienteNombre(), p.getClienteCelular(), p.getDireccion(), p.getReferencia(), p.getNota(),
                    p.getTotal(), p.isPagado(), p.getCreadoEn(), p.getActualizadoEn(),
                    p.getItems().stream().map(ItemDto::de).toList());
        }

        /** Lo que ve el comensal o el cliente: sin el celular ni la dirección de nadie. */
        static PedidoDto publico(Pedido p) {
            PedidoDto d = de(p);
            return new PedidoDto(d.id(), d.codigo(), d.numero(), d.tipo(), d.estado(), d.mesa(), null, d.clienteNombre(),
                    null, null, null, d.nota(), d.total(), d.pagado(), d.creadoEn(), d.actualizadoEn(), d.items());
        }
    }

    public record LineaIn(@NotNull Long platoId, @Min(1) @Max(30) int cantidad, @Size(max = 120) String nota) {
        PedidoService.Linea aLinea() {
            return new PedidoService.Linea(platoId, cantidad, nota);
        }
    }

    public record PedidoMesaIn(@NotEmpty @Size(max = 30) List<@Valid LineaIn> lineas, @Size(max = 240) String nota) {
    }

    public record PedidoMozoIn(@NotNull Long mesaId, @NotEmpty @Size(max = 30) List<@Valid LineaIn> lineas,
                               @Size(max = 240) String nota) {
    }

    public record ClienteIn(@NotBlank @Size(max = 80) String nombre,
                            @NotBlank @Pattern(regexp = "9\\d{8}", message = "El celular tiene 9 dígitos y empieza con 9.") String celular,
                            @Size(max = 160) String direccion, @Size(max = 160) String referencia) {
    }

    public record PedidoExternoIn(@NotNull Pedido.Tipo tipo, @NotNull @Valid ClienteIn cliente,
                                  @NotEmpty @Size(max = 30) List<@Valid LineaIn> lineas, @Size(max = 240) String nota) {
    }

    public record ItemEstadoIn(@NotNull ItemPedido.Estado estado) {
    }

    public record MesaPublicaDto(int numero, String zona, Atencion.Estado estadoCuenta, BigDecimal total,
                                 List<PedidoDto> pedidos) {
    }

    // ---------- Mesas y caja ----------

    public record AtencionDto(Long id, Atencion.Estado estado, LocalDateTime abiertaEn, LocalDateTime cuentaEn,
                              BigDecimal consumo, int porConfirmar, int listos) {
    }

    public record MesaDto(Long id, int numero, String zona, int capacidad, String token, AtencionDto atencion) {
    }

    public record CuentaDto(Long atencionId, int mesa, Atencion.Estado estado, List<PedidoDto> pedidos,
                            BigDecimal total, BigDecimal pagado, BigDecimal saldo) {
    }

    public record CobroMesaIn(@NotNull Pago.Medio medio, @NotNull @DecimalMin("0.10") BigDecimal monto,
                              BigDecimal recibido) {
    }

    public record CobroPedidoIn(@NotNull Pago.Medio medio, BigDecimal recibido) {
    }

    public record PagoDto(Long id, Pago.Medio medio, BigDecimal monto, BigDecimal recibido, BigDecimal vuelto,
                          String cobradoPor, LocalDateTime creadoEn) {
        static PagoDto de(Pago p) {
            return new PagoDto(p.getId(), p.getMedio(), p.getMonto(), p.getRecibido(), p.getVuelto(), p.getCobradoPor(),
                    p.getCreadoEn());
        }
    }

    public record CierreIn(@NotNull LocalDate fecha, @NotNull @DecimalMin("0.00") BigDecimal efectivoContado) {
    }

    public record CierreDto(LocalDate fecha, BigDecimal totalEfectivo, BigDecimal totalYape, BigDecimal totalPlin,
                            BigDecimal totalTarjeta, BigDecimal total, BigDecimal efectivoContado, BigDecimal diferencia,
                            int tickets, String cerradoPor, LocalDateTime cerradoEn) {
        static CierreDto de(CierreCaja c) {
            return c == null ? null : new CierreDto(c.getFecha(), c.getTotalEfectivo(), c.getTotalYape(), c.getTotalPlin(),
                    c.getTotalTarjeta(), c.total(), c.getEfectivoContado(), c.getDiferencia(), c.getTickets(),
                    c.getCerradoPor(), c.getCerradoEn());
        }
    }

    // ---------- Sesión ----------

    public record LoginIn(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record UsuarioDto(String email, String nombre, Usuario.Rol rol) {
        static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getEmail(), u.getNombre(), u.getRol());
        }
    }

    public record LoginOut(String token, Instant expira, UsuarioDto usuario) {
    }
}
