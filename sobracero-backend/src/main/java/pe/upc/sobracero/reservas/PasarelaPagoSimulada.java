package pe.upc.sobracero.reservas;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Pasarela simulada: aprueba todo salvo el método de prueba TARJETA_RECHAZADA (tarjeta sin fondos). */
@Component
public class PasarelaPagoSimulada implements PasarelaPago {

    @Override
    public void cobrar(BigDecimal monto, String metodo) {
        if ("TARJETA_RECHAZADA".equalsIgnoreCase(metodo)) {
            throw new PagoRechazadoException();
        }
    }
}
