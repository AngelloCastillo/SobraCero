package pe.upc.sobracero.donaciones;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.sobracero.comercios.ComercioRepository;
import pe.upc.sobracero.notificaciones.NotificacionService;
import pe.upc.sobracero.ofertas.Oferta;
import pe.upc.sobracero.ofertas.OfertaRepository;
import pe.upc.sobracero.reservas.Reserva;
import pe.upc.sobracero.reservas.ReservaRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ciclo de vida automático de ofertas, reservas y donaciones. Se ejecuta cada minuto
 * (y se puede invocar directamente en las pruebas).
 */
@Service
@RequiredArgsConstructor
public class CicloVidaService {

    private final OfertaRepository ofertas;
    private final ReservaRepository reservas;
    private final DonacionRepository donaciones;
    private final DonacionService donacionService;
    private final ComercioRepository comercios;
    private final NotificacionService notificaciones;
    private final Clock reloj;

    @Value("${app.donacion.minutos-previos}")
    private long minutosPrevios;

    @Value("${app.recordatorio.minutos-antes-cierre}")
    private long minutosRecordatorio;

    @Transactional
    public void ejecutar() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        recordarYCerrarReservas(ahora);
        cerrarDonaciones(ahora);
        derivarADonacion(ahora);
        vencerOfertas(ahora);
    }

    /** US10: recordatorio 30 min antes del cierre y marcado de reservas no recogidas. */
    private void recordarYCerrarReservas(LocalDateTime ahora) {
        for (Reserva r : reservas.buscarPagadasQueCierranAntesDe(ahora.plusMinutes(minutosRecordatorio))) {
            Oferta o = ofertas.findById(r.getOfertaId()).orElseThrow();
            if (!o.getRecojoHasta().isAfter(ahora)) {
                r.setEstado(Reserva.NO_RECOGIDA);
                notificaciones.notificar(r.getConsumidorId(), "Reserva no recogida",
                        "No recogiste \"" + o.getTitulo() + "\" a tiempo. La reserva se marcó como no recogida.");
                reservas.save(r);
            } else if (!r.isRecordatorioEnviado()) {
                var c = comercios.findById(o.getComercioId()).orElseThrow();
                notificaciones.notificar(r.getConsumidorId(), "Tu recojo cierra pronto",
                        "Recoge \"" + o.getTitulo() + "\" en " + c.getDireccion() + " antes de las "
                                + o.getRecojoHasta().toLocalTime() + ". Código: " + r.getCodigo());
                r.setRecordatorioEnviado(true);
                reservas.save(r);
            }
        }
    }

    /** Donaciones que nadie recogió a tiempo: se cierran y la oferta queda como pérdida. */
    private void cerrarDonaciones(LocalDateTime ahora) {
        for (Donacion d : donaciones.buscarAbiertasVencidas(ahora)) {
            d.setEstado(Donacion.RECHAZADA);
            donaciones.save(d);
            ofertas.findById(d.getOfertaId()).ifPresent(o -> {
                o.setEstado(Oferta.PERDIDA);
                ofertas.save(o);
            });
        }
    }

    /** US04: ofertas sin reservar cuya ventana está por cerrar pasan a donación. */
    private void derivarADonacion(LocalDateTime ahora) {
        LocalDateTime limite = ahora.plusMinutes(minutosPrevios);
        for (Oferta o : ofertas.findByEstadoAndCantidadGreaterThanAndRecojoHastaLessThanEqual(Oferta.ACTIVA, 0, limite)) {
            if (o.getRecojoHasta().isAfter(ahora)) {
                donacionService.derivar(o);
            } else {
                o.setEstado(Oferta.PERDIDA);   // la tarea no alcanzó a derivarla antes del cierre
                ofertas.save(o);
            }
        }
    }

    private void vencerOfertas(LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        for (Oferta o : ofertas.findByEstadoAndVenceElBefore(Oferta.ACTIVA, hoy)) {
            o.setEstado(Oferta.VENCIDA);
            ofertas.save(o);
        }
    }
}
