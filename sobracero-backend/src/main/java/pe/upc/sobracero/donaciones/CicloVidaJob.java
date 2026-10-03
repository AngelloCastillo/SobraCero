package pe.upc.sobracero.donaciones;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.tareas.activadas", havingValue = "true", matchIfMissing = true)
public class CicloVidaJob {

    private final CicloVidaService cicloVida;

    @Scheduled(fixedDelayString = "${app.tareas.intervalo-ms:60000}", initialDelayString = "${app.tareas.intervalo-ms:60000}")
    public void ejecutar() {
        try {
            cicloVida.ejecutar();
        } catch (Exception e) {
            log.error("Error en el ciclo de vida automático", e);
        }
    }
}
