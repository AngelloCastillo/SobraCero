package pe.upc.sobracero.prediccion;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.upc.sobracero.usuarios.UsuarioActual;

@RestController
@RequestMapping("/api/prediccion")
@RequiredArgsConstructor
@Tag(name = "Predicción")
public class PrediccionController {

    private final PrediccionService servicio;

    @GetMapping("/comercio")
    @PreAuthorize("hasRole('COMERCIO')")
    public PrediccionService.Prediccion comercio() {
        return servicio.deComercio(UsuarioActual.id());
    }
}
