package pe.upc.sobracero.comercios;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.upc.sobracero.comun.EstadoCuenta;
import pe.upc.sobracero.comun.ExcepcionApi;
import pe.upc.sobracero.notificaciones.NotificacionService;
import pe.upc.sobracero.usuarios.UsuarioActual;

import java.util.List;

@RestController
@RequestMapping("/api/comercios")
@RequiredArgsConstructor
@Tag(name = "Comercios")
public class ComercioController {

    private final ComercioRepository repositorio;
    private final NotificacionService notificaciones;

    public record RechazoRequest(@NotBlank(message = "Indica el motivo del rechazo") String motivo) {}
    public record PlanRequest(@NotBlank(message = "Indica el plan") String plan) {}

    /** Datos del comercio del usuario autenticado. */
    @GetMapping("/yo")
    @PreAuthorize("hasRole('COMERCIO')")
    public Comercio yo() {
        return repositorio.findByUsuarioId(UsuarioActual.id())
                .orElseThrow(() -> ExcepcionApi.noEncontrado("Comercio no encontrado"));
    }

    /** Administrador: listado (opcionalmente filtrado por estado). */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<Comercio> listar(@RequestParam(required = false) EstadoCuenta estado) {
        return estado == null ? repositorio.findAll() : repositorio.findByEstadoOrderByCreadoEnDesc(estado);
    }

    /** US18: el administrador aprueba la cuenta. */
    @PatchMapping("/{id}/validar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Comercio validar(@PathVariable Long id) {
        Comercio c = buscar(id);
        c.setEstado(EstadoCuenta.VALIDADA);
        c.setMotivoRechazo(null);
        notificaciones.notificar(c.getUsuarioId(), "Cuenta validada", "Tu comercio fue validado. Ya puedes publicar ofertas.");
        return repositorio.save(c);
    }

    /** US18: rechazo con motivo específico. */
    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Comercio rechazar(@PathVariable Long id, @Valid @RequestBody RechazoRequest req) {
        Comercio c = buscar(id);
        c.setEstado(EstadoCuenta.RECHAZADA);
        c.setMotivoRechazo(req.motivo());
        notificaciones.notificar(c.getUsuarioId(), "Cuenta rechazada", "Motivo: " + req.motivo());
        return repositorio.save(c);
    }

    /** Administrador: cambia el plan del comercio (GRATIS / PREMIUM). */
    @PatchMapping("/{id}/plan")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Comercio plan(@PathVariable Long id, @Valid @RequestBody PlanRequest req) {
        if (!List.of("GRATIS", "PREMIUM").contains(req.plan())) {
            throw ExcepcionApi.solicitudInvalida("Plan inválido (GRATIS o PREMIUM)");
        }
        Comercio c = buscar(id);
        c.setPlan(req.plan());
        return repositorio.save(c);
    }

    private Comercio buscar(Long id) {
        return repositorio.findById(id).orElseThrow(() -> ExcepcionApi.noEncontrado("Comercio no encontrado"));
    }
}
