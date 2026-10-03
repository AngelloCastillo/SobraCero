package pe.upc.sobracero.notificaciones;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findTop30ByUsuarioIdOrderByIdDesc(Long usuarioId);
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    @Modifying
    @Query("update Notificacion n set n.leida = true where n.usuarioId = :usuarioId and n.leida = false")
    int marcarTodasLeidas(Long usuarioId);
}
