package pe.upc.sobracero.ofertas;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    /**
     * US06/US07: ofertas activas, no vencidas y con ventana abierta, dentro de un radio (km) usando Haversine.
     * Ordenadas por distancia. Se puede migrar a PostGIS más adelante.
     */
    @Query(value = """
            SELECT o.* FROM oferta o
            JOIN comercio c ON c.id = o.comercio_id
            WHERE o.estado = 'ACTIVA'
              AND o.cantidad > 0
              AND c.estado = 'VALIDADA'
              AND o.vence_el >= :hoy
              AND o.recojo_hasta > :ahora
              AND (6371 * acos(LEAST(1.0,
                    cos(radians(:lat)) * cos(radians(c.latitud)) * cos(radians(c.longitud) - radians(:lng))
                  + sin(radians(:lat)) * sin(radians(c.latitud))))) <= :radioKm
            ORDER BY (6371 * acos(LEAST(1.0,
                    cos(radians(:lat)) * cos(radians(c.latitud)) * cos(radians(c.longitud) - radians(:lng))
                  + sin(radians(:lat)) * sin(radians(c.latitud)))))
            """, nativeQuery = true)
    List<Oferta> buscarCercanas(@Param("lat") double lat, @Param("lng") double lng, @Param("radioKm") double radioKm,
                                @Param("ahora") LocalDateTime ahora, @Param("hoy") LocalDate hoy);

    /** Bloqueo pesimista para evitar sobreventa al reservar. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Oferta o where o.id = :id")
    Optional<Oferta> buscarPorIdParaActualizar(@Param("id") Long id);

    List<Oferta> findByComercioIdOrderByIdDesc(Long comercioId);

    List<Oferta> findByEstadoAndCantidadGreaterThanAndRecojoHastaLessThanEqual(String estado, int cantidad, LocalDateTime limite);

    List<Oferta> findByEstadoAndVenceElBefore(String estado, LocalDate fecha);
}
