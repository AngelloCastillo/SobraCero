package pe.upc.sobracero.notificaciones;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.upc.sobracero.usuarios.UsuarioActual;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones")
public class NotificacionController {

    private final NotificacionRepository repositorio;

    public record NotificacionesVista(long sinLeer, List<Notificacion> items) {}

    @GetMapping
    public NotificacionesVista mias() {
        Long uid = UsuarioActual.id();
        return new NotificacionesVista(repositorio.countByUsuarioIdAndLeidaFalse(uid),
                repositorio.findTop30ByUsuarioIdOrderByIdDesc(uid));
    }

    @PostMapping("/leer-todas")
    @Transactional
    public void leerTodas() {
        repositorio.marcarTodasLeidas(UsuarioActual.id());
    }
}
