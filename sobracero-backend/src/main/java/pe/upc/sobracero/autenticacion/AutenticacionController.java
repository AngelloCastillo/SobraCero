package pe.upc.sobracero.autenticacion;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.upc.sobracero.usuarios.UsuarioActual;

@RestController
@RequestMapping("/api/autenticacion")
@RequiredArgsConstructor
@Tag(name = "Autenticación")
public class AutenticacionController {

    private final AutenticacionService servicio;

    /** Registro de consumidor, comercio u organización social. Comercios y organizaciones nacen PENDIENTE. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AutenticacionDtos.RespuestaAutenticacion registro(@Valid @RequestBody AutenticacionDtos.RegistroRequest req) {
        return servicio.registrar(req);
    }

    @PostMapping("/login")
    public AutenticacionDtos.RespuestaAutenticacion login(@Valid @RequestBody AutenticacionDtos.LoginRequest req) {
        return servicio.login(req);
    }

    @GetMapping("/yo")
    public AutenticacionDtos.UsuarioVista yo() {
        return servicio.yo(UsuarioActual.id());
    }
}
