package pe.upc.sobracero.donaciones;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DonacionRepository extends JpaRepository<Donacion, Long> {
    Optional<Donacion> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<Donacion> findByOfertaId(Long ofertaId);
    List<Donacion> findByOrganizacionIdOrderByIdDesc(Long organizacionId);

    /** Kilos comprometidos por una organización para donaciones que cierran en [desde, hasta). */
    @Query("""
            select coalesce(sum(d.pesoKg), 0) from Donacion d, Oferta o
            where d.ofertaId = o.id and d.organizacionId = :orgId
              and d.estado in ('OFRECIDA','ACEPTADA','COMPLETADA')
              and o.recojoHasta >= :desde and o.recojoHasta < :hasta
            """)
    BigDecimal kgComprometidos(@Param("orgId") Long orgId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Donaciones abiertas cuya ventana de recojo ya cerró. */
    @Query("""
            select d from Donacion d, Oferta o
            where d.ofertaId = o.id and d.estado in ('OFRECIDA','ACEPTADA') and o.recojoHasta < :ahora
            """)
    List<Donacion> buscarAbiertasVencidas(@Param("ahora") LocalDateTime ahora);

    @Query("""
            select d from Donacion d, Oferta o
            where d.ofertaId = o.id and o.comercioId = :comercioId order by d.id desc
            """)
    List<Donacion> buscarPorComercio(@Param("comercioId") Long comercioId);
}
