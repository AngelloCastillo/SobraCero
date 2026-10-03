package pe.upc.sobracero.comun;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/** Convierte las excepciones en respuestas JSON con el campo "detail" en español. */
@RestControllerAdvice
public class ManejadorExcepciones {

    @ExceptionHandler(ExcepcionApi.class)
    ProblemDetail api(ExcepcionApi ex) {
        return ProblemDetail.forStatusAndDetail(ex.estado(), ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail solicitudInvalida(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Ya existe un registro con esos datos (correo o RUC duplicado)");
    }
}
