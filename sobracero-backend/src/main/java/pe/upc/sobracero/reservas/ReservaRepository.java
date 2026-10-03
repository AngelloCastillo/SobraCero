package pe.upc.sobracero.reservas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    Optional<Reserva> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<Reserva> findByOfertaIdIn(Collection<Long> ofertaIds);
    List<Reserva> findByConsumidorIdOrderByIdDesc(Long consumidorId);

    /** Reservas pagadas cuya ventana de recojo cierra antes de `limite` (para recordatorios / no recogidas). */
    @Query("select r from Reserva r, Oferta o where r.ofertaId = o.id and r.estado = 'PAGADA' and o.recojoHasta <= :limite")
    List<Reserva> buscarPagadasQueCierranAntesDe(@Param("limite") LocalDateTime limite);
}
