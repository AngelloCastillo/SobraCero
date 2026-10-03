package pe.upc.sobracero.comun;

import org.springframework.http.HttpStatus;

/** Error de negocio con código HTTP asociado. */
public class ExcepcionApi extends RuntimeException {
    private final HttpStatus estado;

    public ExcepcionApi(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus estado() {
        return estado;
    }

    public static ExcepcionApi noEncontrado(String m) { return new ExcepcionApi(HttpStatus.NOT_FOUND, m); }
    public static ExcepcionApi solicitudInvalida(String m) { return new ExcepcionApi(HttpStatus.BAD_REQUEST, m); }
    public static ExcepcionApi conflicto(String m) { return new ExcepcionApi(HttpStatus.CONFLICT, m); }
    public static ExcepcionApi prohibido(String m) { return new ExcepcionApi(HttpStatus.FORBIDDEN, m); }
}
