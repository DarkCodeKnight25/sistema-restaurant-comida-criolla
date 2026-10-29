package pe.dvega.restaurant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import pe.dvega.restaurant.config.TiempoConfig;

import java.time.Clock;
import java.time.LocalDateTime;

/** Las pruebas corren siempre el jueves 29 de octubre de 2026 a las 13:30 en Lima, en plena hora punta. */
@TestConfiguration
public class RelojFijo {

    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 29, 13, 30);

    @Bean
    @Primary
    Clock relojFijo() {
        return Clock.fixed(AHORA.atZone(TiempoConfig.LIMA).toInstant(), TiempoConfig.LIMA);
    }
}
