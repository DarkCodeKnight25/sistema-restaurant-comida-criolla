package pe.dvega.restaurant.dominio;

/** Una regla de la agenda impide la operación; se responde con 409 o 422 y un mensaje para recepción. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
