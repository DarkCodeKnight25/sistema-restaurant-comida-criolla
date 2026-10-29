package pe.dvega.restaurant.servicio;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.dvega.restaurant.dominio.ContadorDia;
import pe.dvega.restaurant.repositorio.ContadorDiaRepositorio;

import java.time.LocalDate;

/** Da el siguiente número de pedido del día sin repetir, aunque lleguen dos pedidos a la vez. */
@Component
public class Numerador {

    private final ContadorDiaRepositorio contadores;
    private final CreadorContador creador;

    public Numerador(ContadorDiaRepositorio contadores, CreadorContador creador) {
        this.contadores = contadores;
        this.creador = creador;
    }

    /** Debe llamarse dentro de la transacción del pedido: la fila queda bloqueada hasta el commit. */
    @Transactional(propagation = Propagation.MANDATORY)
    public int siguiente(LocalDate fecha) {
        try {
            creador.asegurar(fecha);
        } catch (DataIntegrityViolationException yaExiste) {
            // Otro pedido simultáneo creó el contador del día; seguimos con esa fila.
        }
        ContadorDia c = contadores.bloquear(fecha).orElseThrow();
        return c.siguiente();
    }

    @Component
    static class CreadorContador {

        private final ContadorDiaRepositorio contadores;

        CreadorContador(ContadorDiaRepositorio contadores) {
            this.contadores = contadores;
        }

        /** Crea la fila del día en su propia transacción; si otro la crea a la vez, lanza la violación de clave. */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void asegurar(LocalDate fecha) {
            if (!contadores.existsById(fecha)) {
                contadores.saveAndFlush(new ContadorDia(fecha));
            }
        }
    }
}
