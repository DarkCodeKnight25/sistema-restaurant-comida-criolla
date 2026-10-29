package pe.dvega.restaurant.dominio;

import java.util.List;

/** El pedido choca con el estado actual (por ejemplo, un plato se agotó mientras el cliente elegía). */
public class ConflictoException extends RuntimeException {

    private final List<Long> platosAgotados;

    public ConflictoException(String mensaje, List<Long> platosAgotados) {
        super(mensaje);
        this.platosAgotados = platosAgotados;
    }

    public List<Long> getPlatosAgotados() {
        return platosAgotados;
    }
}
