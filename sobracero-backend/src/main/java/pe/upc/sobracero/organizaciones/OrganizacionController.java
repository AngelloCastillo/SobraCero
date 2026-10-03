package pe.upc.sobracero.organizaciones;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.upc.sobracero.comun.EstadoCuenta;
import pe.upc.sobracero.comun.ExcepcionApi;
import pe.upc.sobracero.notificaciones.NotificacionService;
import pe.upc.sobracero.usuarios.UsuarioActual;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/organizaciones")
@RequiredArgsConstructor
@Tag(name = "Organizaciones sociales")
public class OrganizacionController {

    private final OrganizacionSocialRepository repositorio;
    private final NotificacionService notificaciones;

    public record CapacidadRequest(@DecimalMin(value = "0", message = "La capacidad no puede ser negativa") BigDecimal capacidadDiariaKg) {}
    public record ValidarRequest(boolean deducibleSunat) {}
    public record RechazoRequest(@NotBlank(message = "Indica el motivo del rechazo") String motivo) {}

    @GetMapping("/yo")
    @PreAuthorize("hasRole('ORGANIZACION')")
    public OrganizacionSocial yo() {
        return repositorio.findByUsuarioId(UsuarioActual.id())
                .orElseThrow(() -> ExcepcionApi.noEncontrado("Organización no encontrada"));
    }

    /** US14: la organización declara su capacidad diaria de recojo. */
    @PatchMapping("/yo/capacidad")
    @PreAuthorize("hasRole('ORGANIZACION')")
    public OrganizacionSocial capacidad(@Valid @RequestBody CapacidadRequest req) {
        OrganizacionSocial o = yo();
        o.setCapacidadDiariaKg(req.capacidadDiariaKg());
        return repositorio.save(o);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<OrganizacionSocial> listar(@RequestParam(required = false) EstadoCuenta estado) {
        return estado == null ? repositorio.findAll() : repositorio.findByEstado(estado);
    }

    /** US18 + US19: aprueba y, si tiene calificación SUNAT vigente, la marca como apta para donaciones deducibles. */
    @PatchMapping("/{id}/validar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public OrganizacionSocial validar(@PathVariable Long id, @RequestBody(required = false) ValidarRequest req) {
        OrganizacionSocial o = buscar(id);
        o.setEstado(EstadoCuenta.VALIDADA);
        o.setMotivoRechazo(null);
        o.setDeducibleSunat(req != null && req.deducibleSunat());
        notificaciones.notificar(o.getUsuarioId(), "Cuenta validada",
                o.isDeducibleSunat()
                        ? "Tu organización fue validada y es apta para donaciones deducibles."
                        : "Tu organización fue validada para recibir donaciones. Falta la calificación SUNAT para donaciones deducibles.");
        return repositorio.save(o);
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public OrganizacionSocial rechazar(@PathVariable Long id, @Valid @RequestBody RechazoRequest req) {
        OrganizacionSocial o = buscar(id);
        o.setEstado(EstadoCuenta.RECHAZADA);
        o.setMotivoRechazo(req.motivo());
        notificaciones.notificar(o.getUsuarioId(), "Cuenta rechazada", "Motivo: " + req.motivo());
        return repositorio.save(o);
    }

    private OrganizacionSocial buscar(Long id) {
        return repositorio.findById(id).orElseThrow(() -> ExcepcionApi.noEncontrado("Organización no encontrada"));
    }
}
