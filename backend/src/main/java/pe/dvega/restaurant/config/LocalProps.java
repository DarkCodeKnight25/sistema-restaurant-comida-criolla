package pe.dvega.restaurant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos del local que cambian de un restaurante a otro de la serie.
 *
 * @param yapeDemo número que se muestra en el QR de Yape/Plin simulado; nunca es un número real en la demo.
 */
@ConfigurationProperties(prefix = "app.local")
public record LocalProps(
        String nombre,
        String tipo,
        String direccion,
        String distrito,
        String horario,
        String celular,
        String yapeDemo,
        boolean demo
) {
}
