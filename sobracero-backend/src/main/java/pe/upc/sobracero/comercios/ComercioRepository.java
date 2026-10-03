package pe.upc.sobracero.comercios;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.upc.sobracero.comun.EstadoCuenta;

import java.util.List;
import java.util.Optional;

public interface ComercioRepository extends JpaRepository<Comercio, Long> {
    Optional<Comercio> findByUsuarioId(Long usuarioId);
    List<Comercio> findByEstadoOrderByCreadoEnDesc(EstadoCuenta estado);
    long countByEstado(EstadoCuenta estado);
}
