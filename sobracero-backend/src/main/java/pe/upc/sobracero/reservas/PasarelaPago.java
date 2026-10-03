package pe.upc.sobracero.reservas;

import java.math.BigDecimal;

/** Pasarela de pago. Aquí se conecta Culqi / Niubiz / Mercado Pago en producción. */
public interface PasarelaPago {

    /** Cobra el monto. Lanza {@link PagoRechazadoException} si es rechazado. */
    void cobrar(BigDecimal monto, String metodo);

    class PagoRechazadoException extends RuntimeException {
        public PagoRechazadoException() {
            super("Pago rechazado, intenta con otro método");
        }
    }
}
