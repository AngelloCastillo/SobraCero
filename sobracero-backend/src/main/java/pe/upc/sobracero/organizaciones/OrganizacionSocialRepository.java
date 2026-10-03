package pe.upc.sobracero.organizaciones;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.upc.sobracero.comun.EstadoCuenta;

import java.util.List;
import java.util.Optional;

public interface OrganizacionSocialRepository extends JpaRepository<OrganizacionSocial, Long> {
    Optional<OrganizacionSocial> findByUsuarioId(Long usuarioId);
    List<OrganizacionSocial> findByEstado(EstadoCuenta estado);
    long countByEstado(EstadoCuenta estado);
}
