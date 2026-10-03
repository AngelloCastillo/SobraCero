package pe.upc.sobracero.reservas;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.upc.sobracero.usuarios.UsuarioActual;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas")
public class ReservaController {

    private final ReservaService servicio;

    public record ReservarRequest(
            @NotNull(message = "Indica la oferta") Long ofertaId,
            @Min(value = 1, message = "La cantidad debe ser al menos 1") int cantidad,
            @NotBlank(message = "Indica el método de pago") String metodoPago) {}

    public record CanjearRequest(@NotBlank(message = "Ingresa el código") String codigo) {}

    /** metodoPago: YAPE, PLIN, TARJETA. Usa TARJETA_RECHAZADA para simular un pago rechazado. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONSUMIDOR')")
    public ReservaService.ReservaVista reservar(@Valid @RequestBody ReservarRequest req) {
        return servicio.reservar(UsuarioActual.id(), req.ofertaId(), req.cantidad(), req.metodoPago());
    }

    @GetMapping("/mias")
    @PreAuthorize("hasRole('CONSUMIDOR')")
    public List<ReservaService.ReservaVista> mias() {
        return servicio.mias(UsuarioActual.id());
    }

    /** El comercio confirma el recojo con el código de reserva. */
    @PostMapping("/canjear")
    @PreAuthorize("hasRole('COMERCIO')")
    public ReservaService.ReservaVista canjear(@Valid @RequestBody CanjearRequest req) {
        return servicio.canjear(UsuarioActual.id(), req.codigo());
    }
}
