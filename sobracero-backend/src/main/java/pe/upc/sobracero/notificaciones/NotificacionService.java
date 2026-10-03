package pe.upc.sobracero.notificaciones;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Notificaciones dentro de la app. Punto único para enviarlas (aquí se puede añadir push/FCM más adelante). */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repositorio;

    public void notificar(Long usuarioId, String titulo, String mensaje) {
        if (usuarioId == null) return;
        Notificacion n = new Notificacion();
        n.setUsuarioId(usuarioId);
        n.setTitulo(titulo);
        n.setMensaje(mensaje.length() > 480 ? mensaje.substring(0, 480) : mensaje);
        repositorio.save(n);
    }
}
