package pe.upc.sobracero.impacto;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.upc.sobracero.usuarios.UsuarioActual;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/impacto")
@RequiredArgsConstructor
@Tag(name = "Impacto")
public class ImpactoController {

    private final ImpactoService servicio;

    @GetMapping("/comercio")
    @PreAuthorize("hasRole('COMERCIO')")
    public ImpactoService.ImpactoComercio comercio() {
        return servicio.deComercio(UsuarioActual.id());
    }

    @GetMapping("/comercio/reporte.csv")
    @PreAuthorize("hasRole('COMERCIO')")
    public ResponseEntity<byte[]> reporteComercio() {
        byte[] cuerpo = servicio.reporteComercioCsv(UsuarioActual.id()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-impacto.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(cuerpo);
    }

    @GetMapping("/organizacion")
    @PreAuthorize("hasRole('ORGANIZACION')")
    public ImpactoService.ImpactoOrganizacion organizacion() {
        return servicio.deOrganizacion(UsuarioActual.id());
    }

    /** Público: cifras agregadas de la plataforma (US20). */
    @GetMapping("/plataforma")
    public ImpactoService.ImpactoPlataforma plataforma() {
        return servicio.plataforma();
    }
}
