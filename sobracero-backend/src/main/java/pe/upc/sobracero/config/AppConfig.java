package pe.upc.sobracero.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class AppConfig {

    /** Reloj con la zona horaria del negocio (Lima). En las pruebas se reemplaza por uno controlable. */
    @Bean
    Clock reloj(@Value("${app.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
