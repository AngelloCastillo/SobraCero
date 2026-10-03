package pe.upc.sobracero.donaciones;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.sobracero.comercios.Comercio;
import pe.upc.sobracero.comercios.ComercioRepository;
import pe.upc.sobracero.comun.EstadoCuenta;
import pe.upc.sobracero.comun.ExcepcionApi;
import pe.upc.sobracero.comun.GeneradorCodigo;
import pe.upc.sobracero.comun.Geo;
import pe.upc.sobracero.notificaciones.NotificacionService;
import pe.upc.sobracero.ofertas.Oferta;
import pe.upc.sobracero.ofertas.OfertaRepository;
import pe.upc.sobracero.organizaciones.OrganizacionSocial;
import pe.upc.sobracero.organizaciones.OrganizacionSocialRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DonacionService {

    private final DonacionRepository donaciones;
    private final OfertaRepository ofertas;
    private final ComercioRepository comercios;
    private final OrganizacionSocialRepository organizaciones;
    private final NotificacionService notificaciones;
    private final Clock reloj;

    public record DonacionVista(Long id, String codigo, String estado, String ofertaTitulo, String categoria,
                                int cantidad, BigDecimal pesoKg, String comercioNombre, String comercioDireccion,
                                double latitud, double longitud, double distanciaKm,
                                LocalDateTime recojoDesde, LocalDateTime recojoHasta, LocalDateTime completadaEn,
                                String organizacionNombre) {}

    /** Fila del registro de donaciones recibidas (formato pensado para la Ley 30498). */
    public record FilaRegistro(LocalDateTime fecha, String donante, String donanteRuc, String tipoAlimento,
                               int cantidad, BigDecimal pesoKg, String beneficiario, boolean deducible, String codigo) {}

    /**
     * US04: deriva la oferta a la organización validada, cercana y con capacidad, que aún no la haya rechazado.
     * Si no hay ninguna, registra la oferta como pérdida no evitada y avisa al comercio.
     */
    @Transactional
    public boolean derivar(Oferta oferta) {
        Comercio comercio = comercios.findById(oferta.getComercioId()).orElseThrow();
        BigDecimal peso = oferta.getPesoUnitarioKg().multiply(BigDecimal.valueOf(oferta.getCantidad()));
        Set<Long> yaIntentadas = donaciones.findByOfertaId(oferta.getId()).stream()
                .map(Donacion::getOrganizacionId).collect(Collectors.toSet());
        LocalDate dia = oferta.getRecojoHasta().toLocalDate();
        LocalDateTime desde = dia.atStartOfDay(), hasta = dia.plusDays(1).atStartOfDay();

        Optional<OrganizacionSocial> mejor = organizaciones.findByEstado(EstadoCuenta.VALIDADA).stream()
                .filter(o -> !yaIntentadas.contains(o.getId()))
                .filter(o -> o.getCapacidadDiariaKg() == null
                        || donaciones.kgComprometidos(o.getId(), desde, hasta).add(peso).compareTo(o.getCapacidadDiariaKg()) <= 0)
                .min(Comparator.comparingDouble(o ->
                        Geo.distanciaKm(comercio.getLatitud(), comercio.getLongitud(), o.getLatitud(), o.getLongitud())));

        if (mejor.isEmpty()) {
            oferta.setEstado(Oferta.PERDIDA);
            ofertas.save(oferta);
            notificaciones.notificar(comercio.getUsuarioId(), "Sin organización disponible",
                    "No se encontró una organización disponible en este momento para \"" + oferta.getTitulo()
                            + "\". El producto se registró como pérdida no evitada.");
            return false;
        }
        OrganizacionSocial org = mejor.get();
        Donacion d = new Donacion();
        d.setOfertaId(oferta.getId());
        d.setOrganizacionId(org.getId());
        d.setCantidad(oferta.getCantidad());
        d.setPesoKg(peso);
        String codigo;
        do { codigo = GeneradorCodigo.generar("DON-", 5); } while (donaciones.existsByCodigo(codigo));
        d.setCodigo(codigo);
        donaciones.save(d);
        oferta.setEstado(Oferta.DONADA);
        ofertas.save(oferta);

        notificaciones.notificar(org.getUsuarioId(), "Donación disponible cerca",
                oferta.getCantidad() + " x " + oferta.getTitulo() + " (" + peso.stripTrailingZeros().toPlainString() + " kg) en "
                        + comercio.getNombre() + ". Recojo hasta las " + oferta.getRecojoHasta().toLocalTime() + ". Código: " + codigo);
        notificaciones.notificar(comercio.getUsuarioId(), "Oferta derivada a donación",
                "\"" + oferta.getTitulo() + "\" fue derivada a " + org.getNombre() + ". Código de donación: " + codigo);
        return true;
    }

    @Transactional
    public DonacionVista aceptar(Long usuarioId, Long donacionId) {
        Donacion d = deLaOrganizacion(usuarioId, donacionId);
        if (!Donacion.OFRECIDA.equals(d.getEstado())) throw ExcepcionApi.conflicto("Esta donación ya no está disponible");
        d.setEstado(Donacion.ACEPTADA);
        donaciones.save(d);
        Oferta o = ofertas.findById(d.getOfertaId()).orElseThrow();
        Comercio c = comercios.findById(o.getComercioId()).orElseThrow();
        notificaciones.notificar(c.getUsuarioId(), "Donación aceptada", nombreOrganizacion(d) + " aceptó recoger \"" + o.getTitulo() + "\".");
        return vistas(List.of(d), organizacionDe(usuarioId)).get(0);
    }

    /** La organización rechaza: se ofrece a la siguiente compatible o se registra como pérdida. */
    @Transactional
    public void rechazar(Long usuarioId, Long donacionId) {
        Donacion d = deLaOrganizacion(usuarioId, donacionId);
        if (!Donacion.OFRECIDA.equals(d.getEstado())) throw ExcepcionApi.conflicto("Esta donación ya no se puede rechazar");
        d.setEstado(Donacion.RECHAZADA);
        donaciones.save(d);
        Oferta oferta = ofertas.findById(d.getOfertaId()).orElseThrow();
        if (oferta.getRecojoHasta().isAfter(LocalDateTime.now(reloj))) derivar(oferta);
        else { oferta.setEstado(Oferta.PERDIDA); ofertas.save(oferta); }
    }

    /** US12: confirma el recojo con el código de la donación. */
    @Transactional
    public DonacionVista completar(Long usuarioId, String codigo) {
        OrganizacionSocial org = organizacionDe(usuarioId);
        Donacion d = donaciones.findByCodigo(codigo.trim().toUpperCase())
                .filter(x -> x.getOrganizacionId().equals(org.getId()))
                .orElseThrow(() -> ExcepcionApi.solicitudInvalida("Código no válido para esta donación"));
        if (Donacion.COMPLETADA.equals(d.getEstado())) throw ExcepcionApi.conflicto("Esta donación ya fue recogida");
        if (Donacion.RECHAZADA.equals(d.getEstado())) throw ExcepcionApi.solicitudInvalida("Código no válido para esta donación");
        d.setEstado(Donacion.COMPLETADA);
        d.setCompletadaEn(LocalDateTime.now(reloj));
        donaciones.save(d);
        Oferta o = ofertas.findById(d.getOfertaId()).orElseThrow();
        Comercio c = comercios.findById(o.getComercioId()).orElseThrow();
        notificaciones.notificar(c.getUsuarioId(), "Donación entregada",
                "\"" + o.getTitulo() + "\" fue recogida por " + org.getNombre() + ". Ya está en tu registro de impacto.");
        return vistas(List.of(d), org).get(0);
    }

    @Transactional(readOnly = true)
    public List<DonacionVista> deOrganizacion(Long usuarioId) {
        OrganizacionSocial org = organizacionDe(usuarioId);
        return vistas(donaciones.findByOrganizacionIdOrderByIdDesc(org.getId()), org);
    }

    @Transactional(readOnly = true)
    public List<DonacionVista> deComercio(Long usuarioId) {
        Comercio c = comercios.findByUsuarioId(usuarioId).orElseThrow(() -> ExcepcionApi.noEncontrado("Comercio no encontrado"));
        return vistas(donaciones.buscarPorComercio(c.getId()), null);
    }

    /** US13: registro automático de donaciones recibidas. */
    @Transactional(readOnly = true)
    public List<FilaRegistro> registro(Long usuarioId) {
        OrganizacionSocial org = organizacionDe(usuarioId);
        List<Donacion> hechas = donaciones.findByOrganizacionIdOrderByIdDesc(org.getId()).stream()
                .filter(d -> Donacion.COMPLETADA.equals(d.getEstado())).toList();
        Map<Long, Oferta> ofertaPorId = mapaOfertas(hechas);
        Map<Long, Comercio> comercioPorId = mapaComercios(ofertaPorId.values());
        return hechas.stream().map(d -> {
            Oferta o = ofertaPorId.get(d.getOfertaId());
            Comercio c = comercioPorId.get(o.getComercioId());
            return new FilaRegistro(d.getCompletadaEn(), c.getNombre(), c.getRuc(), o.getCategoria() + " - " + o.getTitulo(),
                    d.getCantidad(), d.getPesoKg(), org.getNombre(), org.isDeducibleSunat(), d.getCodigo());
        }).toList();
    }

    // ---------- auxiliares ----------

    private OrganizacionSocial organizacionDe(Long usuarioId) {
        return organizaciones.findByUsuarioId(usuarioId).orElseThrow(() -> ExcepcionApi.noEncontrado("Organización no encontrada"));
    }

    private Donacion deLaOrganizacion(Long usuarioId, Long donacionId) {
        OrganizacionSocial org = organizacionDe(usuarioId);
        return donaciones.findById(donacionId)
                .filter(d -> d.getOrganizacionId().equals(org.getId()))
                .orElseThrow(() -> ExcepcionApi.noEncontrado("Donación no encontrada"));
    }

    private String nombreOrganizacion(Donacion d) {
        return organizaciones.findById(d.getOrganizacionId()).map(OrganizacionSocial::getNombre).orElse("La organización");
    }

    private Map<Long, Oferta> mapaOfertas(List<Donacion> lista) {
        return ofertas.findAllById(lista.stream().map(Donacion::getOfertaId).distinct().toList())
                .stream().collect(Collectors.toMap(Oferta::getId, Function.identity()));
    }

    private Map<Long, Comercio> mapaComercios(Collection<Oferta> os) {
        return comercios.findAllById(os.stream().map(Oferta::getComercioId).distinct().toList())
                .stream().collect(Collectors.toMap(Comercio::getId, Function.identity()));
    }

    private List<DonacionVista> vistas(List<Donacion> lista, OrganizacionSocial observador) {
        Map<Long, Oferta> ofertaPorId = mapaOfertas(lista);
        Map<Long, Comercio> comercioPorId = mapaComercios(ofertaPorId.values());
        Map<Long, String> nombresOrg = organizaciones.findAllById(
                        lista.stream().map(Donacion::getOrganizacionId).distinct().toList())
                .stream().collect(Collectors.toMap(OrganizacionSocial::getId, OrganizacionSocial::getNombre));
        return lista.stream().map(d -> {
            Oferta o = ofertaPorId.get(d.getOfertaId());
            Comercio c = comercioPorId.get(o.getComercioId());
            double distancia = observador == null ? 0
                    : Geo.distanciaKm(observador.getLatitud(), observador.getLongitud(), c.getLatitud(), c.getLongitud());
            return new DonacionVista(d.getId(), d.getCodigo(), d.getEstado(), o.getTitulo(), o.getCategoria(),
                    d.getCantidad(), d.getPesoKg(), c.getNombre(), c.getDireccion(), c.getLatitud(), c.getLongitud(), distancia,
                    o.getRecojoDesde(), o.getRecojoHasta(), d.getCompletadaEn(), nombresOrg.get(d.getOrganizacionId()));
        }).toList();
    }
}
