package pe.dvega.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class RestaurantApplication {

    public static void main(String[] args) {
        // El servidor puede correr en UTC (Render); el restaurante siempre piensa en hora de Lima.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
        SpringApplication.run(RestaurantApplication.class, args);
    }
}
