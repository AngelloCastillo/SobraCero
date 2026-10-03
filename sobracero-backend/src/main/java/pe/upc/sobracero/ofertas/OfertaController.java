package pe.upc.sobracero.ofertas;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.upc.sobracero.comercios.Comercio;
import pe.upc.sobracero.comercios.ComercioRepository;
import pe.upc.sobracero.comun.EstadoCuenta;
import pe.upc.sobracero.comun.ExcepcionApi;
import pe.upc.sobracero.comun.Geo;
import pe.upc.sobracero.usuarios.UsuarioActual;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ofertas")
@RequiredArgsConstructor
@Tag(name = "Ofertas")
public class OfertaController {

    private final OfertaRepository ofertas;
    private final ComercioRepository comercios;
    private final Clock reloj;

    public record CrearOfertaRequest(
            @NotBlank(message = "Escribe el nombre del producto") String titulo,
            String descripcion,
            @NotBlank(message = "Selecciona una categoría") String categoria,
            @Min(value = 1, message = "La cantidad debe ser al menos 1") int cantidad,
            @DecimalMin(value = "0.01", message = "Indica el peso por unidad (kg)") BigDecimal pesoUnitarioKg,
            @NotNull(message = "Indica el precio original") @DecimalMin(value = "0.01", message = "El precio original debe ser mayor a 0") BigDecimal precioOriginal,
            @NotNull(message = "Indica el precio con descuento") @DecimalMin(value = "0.00", message = "El precio con descuento no puede ser negativo") BigDecimal precioDescuento,
            @NotNull(message = "Completa la fecha de vencimiento para publicar") LocalDate venceEl,
            @NotNull(message = "Indica la hora de inicio del recojo") LocalDateTime recojoDesde,
            @NotNull(message = "Indica la hora límite del recojo") LocalDateTime recojoHasta) {}

    /** Oferta enriquecida con datos del comercio y distancia, lista para pintar en el mapa. */
    public record OfertaVista(
            Long id, Long comercioId, String comercioNombre, String comercioDireccion,
            double latitud, double longitud, double distanciaKm,
            String titulo, String descripcion, String categoria, int cantidad,
            BigDecimal pesoUnitarioKg, BigDecimal precioOriginal, BigDecimal precioDescuento, LocalDate venceEl,
            LocalDateTime recojoDesde, LocalDateTime recojoHasta) {}

    /** US02 + US03: el comercio validado publica una oferta con ventana de recojo. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMERCIO')")
    public Oferta crear(@Valid @RequestBody CrearOfertaRequest req) {
        Comercio comercio = comercios.findByUsuarioId(UsuarioActual.id())
                .orElseThrow(() -> ExcepcionApi.noEncontrado("Comercio no encontrado"));
        if (comercio.getEstado() != EstadoCuenta.VALIDADA) {
            throw ExcepcionApi.prohibido("Tu cuenta aún no fue validada por el administrador");
        }
        if (!req.recojoHasta().isAfter(req.recojoDesde())) {
            throw ExcepcionApi.solicitudInvalida("La hora límite debe ser posterior a la hora de inicio");
        }
        if (!req.recojoHasta().isAfter(LocalDateTime.now(reloj))) {
            throw ExcepcionApi.solicitudInvalida("La ventana de recojo ya terminó");
        }
        if (req.precioDescuento().compareTo(req.precioOriginal()) >= 0) {
            throw ExcepcionApi.solicitudInvalida("El precio con descuento debe ser menor al precio original");
        }
        Oferta o = new Oferta();
        o.setComercioId(comercio.getId());
        o.setTitulo(req.titulo().trim());
        o.setDescripcion(req.descripcion());
        o.setCategoria(req.categoria());
        o.setCantidad(req.cantidad());
        o.setCantidadInicial(req.cantidad());
        if (req.pesoUnitarioKg() != null) o.setPesoUnitarioKg(req.pesoUnitarioKg());
        o.setPrecioOriginal(req.precioOriginal());
        o.setPrecioDescuento(req.precioDescuento());
        o.setVenceEl(req.venceEl());
        o.setRecojoDesde(req.recojoDesde());
        o.setRecojoHasta(req.recojoHasta());
        return ofertas.save(o);
    }

    /** Ofertas del comercio autenticado (todas, con su estado). */
    @GetMapping("/mias")
    @PreAuthorize("hasRole('COMERCIO')")
    public List<Oferta> mias() {
        Comercio comercio = comercios.findByUsuarioId(UsuarioActual.id())
                .orElseThrow(() -> ExcepcionApi.noEncontrado("Comercio no encontrado"));
        return ofertas.findByComercioIdOrderByIdDesc(comercio.getId());
    }

    /** US06: buscar ofertas cercanas (radio en km, por defecto 1), ordenadas por distancia. Público. */
    @GetMapping("/cercanas")
    public List<OfertaVista> cercanas(@RequestParam double lat,
                                      @RequestParam double lng,
                                      @RequestParam(defaultValue = "1") double radioKm) {
        List<Oferta> encontradas = ofertas.buscarCercanas(lat, lng, radioKm, LocalDateTime.now(reloj), LocalDate.now(reloj));
        return aVistas(encontradas, lat, lng);
    }

    /** US07: detalle con fecha de vencimiento exacta. Público. */
    @GetMapping("/{id}")
    public OfertaVista detalle(@PathVariable Long id) {
        Oferta o = ofertas.findById(id).orElseThrow(() -> ExcepcionApi.noEncontrado("Oferta no encontrada"));
        return aVistas(List.of(o), Double.NaN, Double.NaN).get(0);
    }

    private List<OfertaVista> aVistas(List<Oferta> encontradas, double lat, double lng) {
        Map<Long, Comercio> porId = comercios.findAllById(
                        encontradas.stream().map(Oferta::getComercioId).distinct().toList())
                .stream().collect(Collectors.toMap(Comercio::getId, Function.identity()));
        return encontradas.stream().map(o -> {
            Comercio c = porId.get(o.getComercioId());
            double distancia = Double.isNaN(lat) ? 0 : Geo.distanciaKm(lat, lng, c.getLatitud(), c.getLongitud());
            return new OfertaVista(o.getId(), c.getId(), c.getNombre(), c.getDireccion(),
                    c.getLatitud(), c.getLongitud(), distancia,
                    o.getTitulo(), o.getDescripcion(), o.getCategoria(), o.getCantidad(),
                    o.getPesoUnitarioKg(), o.getPrecioOriginal(), o.getPrecioDescuento(), o.getVenceEl(),
                    o.getRecojoDesde(), o.getRecojoHasta());
        }).toList();
    }
}
