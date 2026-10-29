package pe.dvega.restaurant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TiempoConfig {

    public static final ZoneId LIMA = ZoneId.of("America/Lima");

    /** Todo el sistema calcula "ahora" con este reloj; las pruebas lo reemplazan por uno fijo. */
    @Bean
    Clock reloj() {
        return Clock.system(LIMA);
    }
}
