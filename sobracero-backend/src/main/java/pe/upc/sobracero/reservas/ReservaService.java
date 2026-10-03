package pe.upc.sobracero.reservas;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.sobracero.comercios.Comercio;
import pe.upc.sobracero.comercios.ComercioRepository;
import pe.upc.sobracero.comun.ExcepcionApi;
import pe.upc.sobracero.comun.GeneradorCodigo;
import pe.upc.sobracero.notificaciones.NotificacionService;
import pe.upc.sobracero.ofertas.Oferta;
import pe.upc.sobracero.ofertas.OfertaRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservas;
    private final OfertaRepository ofertas;
    private final ComercioRepository comercios;
    private final PasarelaPago pagos;
    private final NotificacionService notificaciones;
    private final Clock reloj;

    public record ReservaVista(Long id, String codigo, String estado, int cantidad, BigDecimal monto,
                               Long ofertaId, String ofertaTitulo, String comercioNombre, String comercioDireccion,
                               LocalDateTime recojoDesde, LocalDateTime recojoHasta, LocalDateTime creadoEn) {}

    /** US08 + US09: reserva y pago. Bloquea la oferta para no vender más de lo disponible. */
    @Transactional
    public ReservaVista reservar(Long consumidorId, Long ofertaId, int cantidad, String metodoPago) {
        if (cantidad < 1) throw ExcepcionApi.solicitudInvalida("La cantidad debe ser al menos 1");
        Oferta oferta = ofertas.buscarPorIdParaActualizar(ofertaId)
                .orElseThrow(() -> ExcepcionApi.noEncontrado("Oferta no encontrada"));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (!Oferta.ACTIVA.equals(oferta.getEstado()) || oferta.getCantidad() <= 0
                || !oferta.getRecojoHasta().isAfter(ahora) || oferta.getVenceEl().isBefore(LocalDate.now(reloj))) {
            throw ExcepcionApi.conflicto("La oferta ya no está disponible");
        }
        if (oferta.getCantidad() < cantidad) {
            throw ExcepcionApi.conflicto("Solo quedan " + oferta.getCantidad() + " unidades disponibles");
        }
        BigDecimal monto = oferta.getPrecioDescuento().multiply(BigDecimal.valueOf(cantidad));
        try {
            pagos.cobrar(monto, metodoPago);
        } catch (PasarelaPago.PagoRechazadoException e) {
            throw new ExcepcionApi(HttpStatus.PAYMENT_REQUIRED, e.getMessage());
        }

        oferta.setCantidad(oferta.getCantidad() - cantidad);
        if (oferta.getCantidad() == 0) oferta.setEstado(Oferta.AGOTADA);
        ofertas.save(oferta);

        Reserva r = new Reserva();
        r.setOfertaId(oferta.getId());
        r.setConsumidorId(consumidorId);
        r.setCantidad(cantidad);
        r.setMonto(monto);
        r.setCodigo(codigoUnico());
        r = reservas.save(r);

        Comercio c = comercios.findById(oferta.getComercioId()).orElseThrow();
        notificaciones.notificar(c.getUsuarioId(), "Nueva reserva",
                cantidad + " x " + oferta.getTitulo() + " reservado. Código: " + r.getCodigo());
        return vista(r, oferta, c);
    }

    @Transactional(readOnly = true)
    public List<ReservaVista> mias(Long consumidorId) {
        List<Reserva> lista = reservas.findByConsumidorIdOrderByIdDesc(consumidorId);
        Map<Long, Oferta> ofertaPorId = ofertas.findAllById(lista.stream().map(Reserva::getOfertaId).distinct().toList())
                .stream().collect(Collectors.toMap(Oferta::getId, Function.identity()));
        Map<Long, Comercio> comercioPorId = comercios.findAllById(
                        ofertaPorId.values().stream().map(Oferta::getComercioId).distinct().toList())
                .stream().collect(Collectors.toMap(Comercio::getId, Function.identity()));
        return lista.stream().map(r -> {
            Oferta o = ofertaPorId.get(r.getOfertaId());
            return vista(r, o, comercioPorId.get(o.getComercioId()));
        }).toList();
    }

    /** US09: el comercio canjea el código; un código ya usado no permite un segundo recojo. */
    @Transactional
    public ReservaVista canjear(Long usuarioComercioId, String codigo) {
        Comercio comercio = comercios.findByUsuarioId(usuarioComercioId)
                .orElseThrow(() -> ExcepcionApi.noEncontrado("Comercio no encontrado"));
        Reserva r = reservas.findByCodigo(codigo.trim().toUpperCase())
                .orElseThrow(() -> ExcepcionApi.noEncontrado("Código no válido"));
        Oferta oferta = ofertas.findById(r.getOfertaId()).orElseThrow();
        if (!oferta.getComercioId().equals(comercio.getId())) {
            throw ExcepcionApi.noEncontrado("Código no válido");
        }
        if (Reserva.RECOGIDA.equals(r.getEstado())) {
            throw ExcepcionApi.conflicto("Este código ya fue utilizado");
        }
        if (Reserva.NO_RECOGIDA.equals(r.getEstado())) {
            throw ExcepcionApi.conflicto("La ventana de recojo de esta reserva ya venció");
        }
        r.setEstado(Reserva.RECOGIDA);
        r.setRecogidaEn(LocalDateTime.now(reloj));
        reservas.save(r);
        notificaciones.notificar(r.getConsumidorId(), "Recojo confirmado", "Recogiste " + oferta.getTitulo() + ". ¡Gracias por rescatar comida!");
        return vista(r, oferta, comercio);
    }

    private String codigoUnico() {
        String codigo;
        do {
            codigo = GeneradorCodigo.generar("SC-", 5);
        } while (reservas.existsByCodigo(codigo));
        return codigo;
    }

    private ReservaVista vista(Reserva r, Oferta o, Comercio c) {
        return new ReservaVista(r.getId(), r.getCodigo(), r.getEstado(), r.getCantidad(), r.getMonto(),
                o.getId(), o.getTitulo(), c.getNombre(), c.getDireccion(), o.getRecojoDesde(), o.getRecojoHasta(), r.getCreadoEn());
    }
}
