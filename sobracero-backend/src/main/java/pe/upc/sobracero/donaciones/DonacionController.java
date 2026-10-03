package pe.upc.sobracero.donaciones;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.upc.sobracero.usuarios.UsuarioActual;

import java.util.List;

@RestController
@RequestMapping("/api/donaciones")
@RequiredArgsConstructor
@Tag(name = "Donaciones")
public class DonacionController {

    private final DonacionService servicio;

    public record CompletarRequest(@NotBlank(message = "Ingresa el código de la donación") String codigo) {}

    /** US11: donaciones ofrecidas / aceptadas / completadas de mi organización. */
    @GetMapping("/mias")
    @PreAuthorize("hasRole('ORGANIZACION')")
    public List<DonacionService.DonacionVista> mias() {
        return servicio.deOrganizacion(UsuarioActual.id());
    }

    @PostMapping("/{id}/aceptar")
    @PreAuthorize("hasRole('ORGANIZACION')")
    public DonacionService.DonacionVista aceptar(@PathVariable Long id) {
        return servicio.aceptar(UsuarioActual.id(), id);
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ORGANIZACION')")
    public void rechazar(@PathVariable Long id) {
        servicio.rechazar(UsuarioActual.id(), id);
    }

    /** US12: confirmar el recojo con el código. */
    @PostMapping("/completar")
    @PreAuthorize("hasRole('ORGANIZACION')")
    public DonacionService.DonacionVista completar(@Valid @RequestBody CompletarRequest req) {
        return servicio.completar(UsuarioActual.id(), req.codigo());
    }

    /** US13: registro de donaciones recibidas (Ley 30498). */
    @GetMapping("/registro")
    @PreAuthorize("hasRole('ORGANIZACION')")
    public List<DonacionService.FilaRegistro> registro() {
        return servicio.registro(UsuarioActual.id());
    }

    /** Donaciones generadas por las ofertas de mi comercio. */
    @GetMapping("/comercio")
    @PreAuthorize("hasRole('COMERCIO')")
    public List<DonacionService.DonacionVista> deComercio() {
        return servicio.deComercio(UsuarioActual.id());
    }
}
