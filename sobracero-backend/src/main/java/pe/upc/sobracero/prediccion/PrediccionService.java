package pe.upc.sobracero.prediccion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.sobracero.comercios.Comercio;
import pe.upc.sobracero.comercios.ComercioRepository;
import pe.upc.sobracero.comun.ExcepcionApi;
import pe.upc.sobracero.impacto.ImpactoService;
import pe.upc.sobracero.ofertas.Oferta;
import pe.upc.sobracero.ofertas.OfertaRepository;
import pe.upc.sobracero.reservas.ReservaRepository;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * US01/US15: predicción de excedentes por categoría para mañana, a partir del historial.
 * Modelo simple y explicable: promedio de unidades sobrantes en el mismo día de la semana.
 * Se puede reemplazar por un modelo de ML manteniendo este contrato.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrediccionService {

    static final int MIN_DIAS_HISTORIAL = 15;

    private final ComercioRepository comercios;
    private final OfertaRepository ofertas;
    private final ReservaRepository reservas;
    private final ImpactoService impacto;
    private final Clock reloj;

    public record PrediccionCategoria(String categoria, double unidadesEsperadas, int descuentoSugeridoPct) {}

    public record Prediccion(boolean disponible, String mensaje, LocalDate fechaObjetivo, int diasHistorial,
                             List<PrediccionCategoria> predicciones) {}

    public Prediccion deComercio(Long usuarioId) {
        Comercio c = comercios.findByUsuarioId(usuarioId).orElseThrow(() -> ExcepcionApi.noEncontrado("Comercio no encontrado"));
        impacto.exigirPremium(c);

        LocalDate objetivo = LocalDate.now(reloj).plusDays(1);
        DayOfWeek diaSemana = objetivo.getDayOfWeek();
        List<Oferta> historial = ofertas.findByComercioIdOrderByIdDesc(c.getId());
        long diasHistorial = historial.stream().map(o -> o.getRecojoDesde().toLocalDate()).distinct().count();
        if (diasHistorial < MIN_DIAS_HISTORIAL) {
            return new Prediccion(false, "Aún no hay suficiente historial para predecir. Publica manualmente mientras tanto ("
                    + diasHistorial + " de " + MIN_DIAS_HISTORIAL + " días).", objetivo, (int) diasHistorial, List.of());
        }

        Map<Long, Integer> reservadas = new HashMap<>();
        for (var r : reservas.findByOfertaIdIn(historial.stream().map(Oferta::getId).toList())) {
            if (!"NO_RECOGIDA".equals(r.getEstado())) reservadas.merge(r.getOfertaId(), r.getCantidad(), Integer::sum);
        }
        // (categoría → fecha → sobrante del día) solo para el mismo día de la semana
        Map<String, Map<LocalDate, Integer>> sobranteCategoriaDia = new TreeMap<>();
        Map<String, List<Integer>> descuentos = new HashMap<>();
        for (Oferta o : historial) {
            int sobro = Math.max(0, o.getCantidadInicial() - reservadas.getOrDefault(o.getId(), 0));
            LocalDate d = o.getRecojoDesde().toLocalDate();
            descuentos.computeIfAbsent(o.getCategoria(), k -> new ArrayList<>())
                    .add((int) Math.round((1 - o.getPrecioDescuento().doubleValue() / o.getPrecioOriginal().doubleValue()) * 100));
            if (d.getDayOfWeek() == diaSemana) {
                sobranteCategoriaDia.computeIfAbsent(o.getCategoria(), k -> new HashMap<>()).merge(d, sobro, Integer::sum);
            }
        }
        List<PrediccionCategoria> resultado = sobranteCategoriaDia.entrySet().stream().map(e -> {
            double promedio = e.getValue().values().stream().mapToInt(Integer::intValue).average().orElse(0);
            int desc = (int) Math.round(descuentos.get(e.getKey()).stream().mapToInt(Integer::intValue).average().orElse(50));
            return new PrediccionCategoria(e.getKey(), Math.round(promedio * 10) / 10.0, desc);
        }).sorted(Comparator.comparingDouble(PrediccionCategoria::unidadesEsperadas).reversed()).collect(Collectors.toList());

        return new Prediccion(true, "Predicción para el " + objetivo + " basada en " + diasHistorial + " días de historial.",
                objetivo, (int) diasHistorial, resultado);
    }
}
