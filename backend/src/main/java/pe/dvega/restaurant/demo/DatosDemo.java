package pe.dvega.restaurant.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import pe.dvega.restaurant.dominio.*;
import pe.dvega.restaurant.repositorio.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Restaurante criollo ficticio «La Sazón de Doña Meche»: carta con fotos reales de Wikimedia Commons (créditos al pie de la carta),
 * 12 mesas y el movimiento de las últimas horas, calculado desde la hora actual para que la demo siempre esté viva.
 * Ningún nombre ni celular es de una persona real.
 */
@Component
public class DatosDemo {

    private static final Logger log = LoggerFactory.getLogger(DatosDemo.class);

    public static final String PASSWORD_DEMO = "demo1234";

    /** Clientes de delivery y para llevar, inventados. */
    private static final String[] CLIENTES = {"Rosa Quispe", "Jorge Salas", "Milagros Huamán", "Renzo Paucar",
            "Fiorella Chávez", "Wilmer Ticona", "Kiara Benites", "Óscar Llanos"};

    private final EstacionRepositorio estaciones;
    private final CategoriaRepositorio categorias;
    private final PlatoRepositorio platos;
    private final MesaRepositorio mesas;
    private final AtencionRepositorio atenciones;
    private final PedidoRepositorio pedidos;
    private final PagoRepositorio pagos;
    private final CierreCajaRepositorio cierres;
    private final ContadorDiaRepositorio contadores;
    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder passwords;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock reloj;
    private final boolean cargar;

    public DatosDemo(EstacionRepositorio estaciones, CategoriaRepositorio categorias, PlatoRepositorio platos,
                     MesaRepositorio mesas, AtencionRepositorio atenciones, PedidoRepositorio pedidos,
                     PagoRepositorio pagos, CierreCajaRepositorio cierres, ContadorDiaRepositorio contadores,
                     UsuarioRepositorio usuarios, PasswordEncoder passwords, JdbcTemplate jdbc, TransactionTemplate tx,
                     Clock reloj, @Value("${app.demo.cargar:true}") boolean cargar) {
        this.estaciones = estaciones;
        this.categorias = categorias;
        this.platos = platos;
        this.mesas = mesas;
        this.atenciones = atenciones;
        this.pedidos = pedidos;
        this.pagos = pagos;
        this.cierres = cierres;
        this.contadores = contadores;
        this.usuarios = usuarios;
        this.passwords = passwords;
        this.jdbc = jdbc;
        this.tx = tx;
        this.reloj = reloj;
        this.cargar = cargar;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        if (cargar && estaciones.count() == 0) {
            tx.executeWithoutResult(s -> sembrar(20261029L));
            log.info("Datos de demostración de La Sazón de Doña Meche cargados");
        }
    }

    /** La demo pública vuelve a su estado inicial cada madrugada. */
    @Scheduled(cron = "${app.demo.reinicio-cron:-}", zone = "America/Lima")
    public void reiniciar() {
        tx.executeWithoutResult(s -> {
            borrarTodo();
            sembrar(LocalDate.now(reloj).toEpochDay());
        });
        log.info("Demo reiniciada");
    }

    public void borrarTodo() {
        for (String t : List.of("pago", "item_pedido", "pedido", "atencion", "cierre_caja", "contador_dia", "plato",
                "categoria", "estacion", "mesa", "usuario")) {
            jdbc.update("delete from " + t);
        }
    }

    public void sembrar(long semilla) {
        Random r = new Random(semilla);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();

        Estacion fogon = estaciones.save(new Estacion("FOGON", "Fogón", "#C8102E", 1));
        Estacion entradas = estaciones.save(new Estacion("ENTRADAS", "Entradas y postres", "#14406B", 2));
        Estacion bar = estaciones.save(new Estacion("BAR", "Bebidas", "#1A1A1A", 3));

        Categoria delDia = categorias.save(new Categoria("Plato del día", 1));
        Categoria entradasCat = categorias.save(new Categoria("Entradas", 2));
        Categoria fondos = categorias.save(new Categoria("Platos de fondo", 3));
        Categoria postres = categorias.save(new Categoria("Postres", 4));
        Categoria bebidas = categorias.save(new Categoria("Bebidas", 5));

        // El plato del día cambia según el día de la semana; solo el de hoy está en la carta.
        String[][] semana = {
                {"Seco con frejoles", "Seco de res al culantro con frejoles canarios y arroz.", "platos/seco-con-frejoles.webp"},
                {"Ají de gallina", "Gallina deshilachada en crema de ají amarillo, con papa, huevo y aceituna.", "platos/aji-de-gallina.webp"},
                {"Carapulcra", "Papa seca guisada con cerdo y maní, con arroz.", "platos/carapulcra.webp"},
                {"Olluquito con carne", "Olluco en tiras guisado con carne y perejil, con arroz.", "platos/olluquito.webp"},
                {"Cau cau", "Mondongo con papa amarilla, palillo y hierbabuena.", "platos/cau-cau.webp"},
                {"Arroz con pollo", "Arroz verde al culantro con presa de pollo y sarsa criolla.", "platos/arroz-con-pollo.webp"},
                {"Rocoto relleno", "Rocoto relleno de carne, con pastel de papa al horno.", "platos/rocoto-relleno.webp"}};
        int hoyDia = hoy.getDayOfWeek().getValue();
        List<Plato> carta = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            String[] x = semana[d - 1];
            Plato p = p(delDia, fogon, x[0], x[1], "18", x[2], d);
            p.setActivo(d == hoyDia);
            carta.add(p);
        }
        carta.add(p(entradasCat, entradas, "Papa a la huancaína", "Papa amarilla bañada en crema de queso y ají amarillo.", "14", "platos/papa-huancaina.webp", 1));
        carta.add(p(entradasCat, fogon, "Anticuchos (2 palitos)", "Corazón de res a la parrilla con papa y choclo.", "16", "platos/anticuchos.webp", 2));
        carta.add(p(entradasCat, entradas, "Causa de pollo", "Papa amarilla con ají, rellena de pollo y mayonesa.", "13", null, 3));
        carta.add(p(fondos, fogon, "Lomo saltado", "Lomo fino salteado al wok con cebolla, tomate y papas fritas.", "32", "platos/lomo-saltado.webp", 1));
        carta.add(p(fondos, fogon, "Tacu tacu con lomo", "Tacu tacu de frejol con bistec, huevo frito y plátano.", "30", "platos/tacu-tacu.webp", 2));
        Plato tallarin = p(fondos, fogon, "Tallarín saltado", "Tallarín al wok con pollo y verduras.", "24", null, 3);
        carta.add(tallarin);
        carta.add(p(fondos, fogon, "Bistec a lo pobre", "Bistec con arroz, papas fritas, huevo y plátano.", "34", null, 4));
        carta.add(p(postres, entradas, "Mazamorra morada", "De maíz morado con frutas y canela.", "8", "platos/mazamorra.webp", 1));
        carta.add(p(postres, entradas, "Arroz con leche", "Con canela y clavo de olor.", "8", "platos/arroz-con-leche.webp", 2));
        carta.add(p(bebidas, bar, "Chicha morada (jarra 1 L)", "Hecha en casa.", "14", null, 1));
        carta.add(p(bebidas, bar, "Maracuyá (jarra 1 L)", null, "14", null, 2));
        carta.add(p(bebidas, bar, "Inca o Coca (personal)", null, "5", null, 3));
        carta.add(p(bebidas, bar, "Agua de cebada (vaso)", null, "4", null, 4));
        // El tallarín se acabó temprano: así la demo muestra el «se terminó».
        tallarin.setDisponible(false);

        List<Mesa> listaMesas = new ArrayList<>();
        for (int n = 1; n <= 12; n++) {
            listaMesas.add(mesas.save(new Mesa(n, token(n), n % 4 == 0 ? 6 : 4, n <= 8 ? "Salón" : "Terraza")));
        }

        usuarios.save(new Usuario("admin@donameche.demo", "Administración", passwords.encode(PASSWORD_DEMO), Usuario.Rol.ADMIN));
        usuarios.save(new Usuario("mozo@donameche.demo", "Kevin (mozo)", passwords.encode(PASSWORD_DEMO), Usuario.Rol.MOZO));
        usuarios.save(new Usuario("cocina@donameche.demo", "Cocina", passwords.encode(PASSWORD_DEMO), Usuario.Rol.COCINA));
        usuarios.save(new Usuario("caja@donameche.demo", "Doña Meche (caja)", passwords.encode(PASSWORD_DEMO), Usuario.Rol.CAJA));

        List<Plato> comida = carta.stream().filter(pl -> pl.getEstacion() != bar && pl.isDisponible() && pl.isActivo()).toList();
        List<Plato> tomar = carta.stream().filter(pl -> pl.getEstacion() == bar).toList();
        int[] numero = {0};

        // Mesas que ya comieron y pagaron. Si la demo se abre fuera del horario (antes de abrir o después de cerrar) (11:00 a 17:00),
        // el historial del día se ubica en el turno de almuerzo para que la caja no muestre ventas con el local cerrado.
        LocalDateTime base = ahora.getHour() >= 16 || ahora.getHour() < 13 ? hoy.atTime(15, 40) : ahora;
        Pago.Medio[] medios = {Pago.Medio.YAPE, Pago.Medio.EFECTIVO, Pago.Medio.YAPE, Pago.Medio.TARJETA, Pago.Medio.PLIN,
                Pago.Medio.EFECTIVO, Pago.Medio.YAPE};
        for (int k = 0; k < 14; k++) {
            Mesa m = listaMesas.get(r.nextInt(listaMesas.size()));
            LocalDateTime inicio = base.minusMinutes(220 - k * 14L + r.nextInt(6));
            Atencion a = atenciones.save(new Atencion(m, inicio));
            Pedido pe = pedido(Pedido.Tipo.MESA, a, inicio, ++numero[0], comida, tomar, r);
            pe.getItems().forEach(i -> i.forzarEstado(ItemPedido.Estado.SERVIDO, inicio.plusMinutes(18)));
            pe.forzarEstado(Pedido.Estado.SERVIDO, inicio.plusMinutes(20));
            pe.marcarPagado();
            LocalDateTime pago = inicio.plusMinutes(48 + r.nextInt(15));
            if (pago.isAfter(base)) {
                pago = base.minusMinutes(2);
            }
            a.pedirCuenta(pago.minusMinutes(4));
            a.cerrar(pago);
            Pago.Medio medio = medios[r.nextInt(medios.length)];
            BigDecimal recibido = medio == Pago.Medio.EFECTIVO
                    ? pe.getTotal().divide(BigDecimal.valueOf(50), 0, RoundingMode.UP).multiply(BigDecimal.valueOf(50)) : null;
            pagos.save(new Pago(a, null, medio, pe.getTotal(), recibido, "Doña Meche (caja)", pago));
        }

        // Mesas ocupadas ahora mismo, cada una en un punto distinto del flujo.
        // Mesa 2: platos listos para llevar a la mesa.
        Atencion m2 = atenciones.save(new Atencion(listaMesas.get(1), ahora.minusMinutes(26)));
        Pedido p2 = pedido(Pedido.Tipo.MESA, m2, ahora.minusMinutes(25), ++numero[0], comida, tomar, r);
        p2.getItems().forEach(i -> i.forzarEstado(ItemPedido.Estado.LISTO, ahora.minusMinutes(3)));
        p2.forzarEstado(Pedido.Estado.LISTO, ahora.minusMinutes(3));
        // Mesa 7: en preparación, con un plato esperando bastante.
        Atencion m7 = atenciones.save(new Atencion(listaMesas.get(6), ahora.minusMinutes(19)));
        Pedido p7 = pedido(Pedido.Tipo.MESA, m7, ahora.minusMinutes(18), ++numero[0], comida, tomar, r);
        p7.getItems().forEach(i -> i.forzarEstado(i.getEstacion() == bar ? ItemPedido.Estado.SERVIDO
                : ItemPedido.Estado.EN_PREPARACION, ahora.minusMinutes(12)));
        p7.forzarEstado(Pedido.Estado.EN_PREPARACION, ahora.minusMinutes(12));
        // Mesa 9: pidió la cuenta.
        Atencion m9 = atenciones.save(new Atencion(listaMesas.get(8), ahora.minusMinutes(55)));
        Pedido p9 = pedido(Pedido.Tipo.MESA, m9, ahora.minusMinutes(54), ++numero[0], comida, tomar, r);
        p9.getItems().forEach(i -> i.forzarEstado(ItemPedido.Estado.SERVIDO, ahora.minusMinutes(30)));
        p9.forzarEstado(Pedido.Estado.SERVIDO, ahora.minusMinutes(30));
        m9.pedirCuenta(ahora.minusMinutes(2));
        // Mesa 11: acaba de pedir y su pedido espera confirmación.
        Atencion m11 = atenciones.save(new Atencion(listaMesas.get(10), ahora.minusMinutes(4)));
        pedido(Pedido.Tipo.MESA, m11, ahora.minusMinutes(3), ++numero[0], comida, tomar, r);
        // Mesa 4: confirmado, todavía nadie lo empieza en cocina.
        Atencion m4 = atenciones.save(new Atencion(listaMesas.get(3), ahora.minusMinutes(9)));
        Pedido p4 = pedido(Pedido.Tipo.MESA, m4, ahora.minusMinutes(8), ++numero[0], comida, tomar, r);
        p4.forzarEstado(Pedido.Estado.CONFIRMADO, ahora.minusMinutes(7));

        // Para llevar ya cobrado y entregado, uno en cocina y un delivery listo sin cobrar.
        Pedido ll1 = externo(Pedido.Tipo.LLEVAR, base.minusMinutes(70), ++numero[0], comida, tomar, r);
        ll1.getItems().forEach(i -> i.forzarEstado(ItemPedido.Estado.LISTO, base.minusMinutes(55)));
        ll1.forzarEstado(Pedido.Estado.ENTREGADO, base.minusMinutes(50));
        ll1.marcarPagado();
        pagos.save(new Pago(null, ll1, Pago.Medio.YAPE, ll1.getTotal(), null, "Doña Meche (caja)", base.minusMinutes(50)));
        Pedido ll2 = externo(Pedido.Tipo.LLEVAR, ahora.minusMinutes(6), ++numero[0], comida, tomar, r);
        ll2.forzarEstado(Pedido.Estado.CONFIRMADO, ahora.minusMinutes(5));
        Pedido dl = externo(Pedido.Tipo.DELIVERY, ahora.minusMinutes(28), ++numero[0], comida, tomar, r);
        dl.getItems().forEach(i -> i.forzarEstado(ItemPedido.Estado.LISTO, ahora.minusMinutes(4)));
        dl.forzarEstado(Pedido.Estado.LISTO, ahora.minusMinutes(4));

        contadores.save(new ContadorDia(hoy, numero[0]));

        // Cierres de los 13 días anteriores, para ver la tendencia de ventas.
        for (int d = 13; d >= 1; d--) {
            LocalDate f = hoy.minusDays(d);
            boolean finDeSemana = f.getDayOfWeek().getValue() >= 6;
            int ventaBase = finDeSemana ? 2600 : 1700;
            BigDecimal total = BigDecimal.valueOf(ventaBase + r.nextInt(500));
            BigDecimal yape = total.multiply(new BigDecimal("0.41")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal plin = total.multiply(new BigDecimal("0.07")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tarjeta = total.multiply(new BigDecimal("0.22")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal efectivo = total.subtract(yape).subtract(plin).subtract(tarjeta);
            BigDecimal contado = efectivo.add(BigDecimal.valueOf(r.nextInt(5) == 0 ? -10 : 0));
            cierres.save(new CierreCaja(f, efectivo, yape, plin, tarjeta, contado, 48 + r.nextInt(40), "Doña Meche (caja)",
                    f.atTime(16, 30)));
        }
    }

    private Plato p(Categoria c, Estacion e, String nombre, String desc, String precio, String foto, int orden) {
        return platos.save(new Plato(c, e, nombre, desc, new BigDecimal(precio), foto, orden));
    }

    private Pedido pedido(Pedido.Tipo tipo, Atencion a, LocalDateTime pedidoEn, int numero, List<Plato> comida,
                          List<Plato> tomar, Random r) {
        // Cerca de la medianoche, un pedido «de hace 25 minutos» caería en el día anterior: se acota al día de hoy.
        LocalDateTime inicioDia = LocalDate.now(reloj).atStartOfDay().plusMinutes(1);
        LocalDateTime cuando = pedidoEn.isBefore(inicioDia) ? inicioDia : pedidoEn;
        Pedido p = new Pedido(codigo(numero), cuando.toLocalDate(), numero, tipo, a, cuando);
        int platosComida = 1 + r.nextInt(3);
        Set<Plato> elegidos = new LinkedHashSet<>();
        while (elegidos.size() < platosComida) {
            elegidos.add(comida.get(r.nextInt(comida.size())));
        }
        elegidos.forEach(pl -> p.agregar(pl, r.nextInt(4) == 0 ? 2 : 1, r.nextInt(6) == 0 ? "Bien picante" : null, cuando));
        p.agregar(tomar.get(r.nextInt(tomar.size())), 1 + r.nextInt(2), null, cuando);
        if (tipo == Pedido.Tipo.MESA) {
            p.forzarEstado(Pedido.Estado.ENVIADO, cuando);
        }
        return pedidos.save(p);
    }

    private Pedido externo(Pedido.Tipo tipo, LocalDateTime cuando, int numero, List<Plato> comida, List<Plato> tomar, Random r) {
        Pedido p = pedido(tipo, null, cuando, numero, comida, tomar, r);
        String nombre = CLIENTES[r.nextInt(CLIENTES.length)];
        String celular = "9" + String.format("%08d", r.nextInt(100_000_000));
        p.datosCliente(nombre, celular, tipo == Pedido.Tipo.DELIVERY ? "Jr. Las Gaviotas 000, Santa Anita (dirección de demostración)" : null,
                tipo == Pedido.Tipo.DELIVERY ? "Puerta verde, frente al parque" : null);
        return p;
    }

    private static String codigo(int numero) {
        return String.format("DM-D%04d", numero);
    }

    /** Tokens fijos para la demo: el QR impreso de cada mesa sigue sirviendo tras cada reinicio. */
    public static String token(int mesa) {
        Random r = new Random(7_000L + mesa);
        String alfabeto = "abcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder("m" + mesa + "-");
        for (int i = 0; i < 8; i++) {
            sb.append(alfabeto.charAt(r.nextInt(alfabeto.length())));
        }
        return sb.toString();
    }
}
