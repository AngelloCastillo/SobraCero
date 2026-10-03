package pe.upc.sobracero.impacto;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.sobracero.comercios.Comercio;
import pe.upc.sobracero.comercios.ComercioRepository;
import pe.upc.sobracero.comun.EstadoCuenta;
import pe.upc.sobracero.comun.ExcepcionApi;
import pe.upc.sobracero.organizaciones.OrganizacionSocial;
import pe.upc.sobracero.organizaciones.OrganizacionSocialRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImpactoService {

    private final EntityManager em;
    private final ComercioRepository comercios;
    private final OrganizacionSocialRepository organizaciones;

    @Value("${app.impacto.co2-kg-por-kg}")
    private BigDecimal factorCo2;

    @Value("${app.impacto.distintivo-min-kg}")
    private BigDecimal distintivoMinKg;

    @Value("${app.impacto.meta-plataforma-kg}")
    private BigDecimal metaPlataformaKg;

    public record Distintivo(boolean otorgado, BigDecimal minKg, BigDecimal avancePct, String mensaje) {}

    public record ImpactoComercio(BigDecimal vendidoKg, BigDecimal donadoKg, BigDecimal rescatadoKg, BigDecimal perdidoKg,
                                  BigDecimal ingresos, BigDecimal co2EvitadoKg, long donacionesCompletadas,
                                  String plan, Distintivo distintivo) {}

    public record ImpactoPlataforma(BigDecimal rescatadoKg, BigDecimal vendidoKg, BigDecimal donadoKg, BigDecimal co2EvitadoKg,
                                    long comerciosActivos, long organizacionesActivas, BigDecimal metaKg, BigDecimal avanceMetaPct) {}

    public record ImpactoOrganizacion(BigDecimal recibidoKg, long donacionesCompletadas, BigDecimal co2EvitadoKg) {}

    /** US05: historial de ventas y donaciones del comercio; US17: distintivo. */
    public ImpactoComercio deComercio(Long usuarioId) {
        Comercio c = comercios.findByUsuarioId(usuarioId).orElseThrow(() -> ExcepcionApi.noEncontrado("Comercio no encontrado"));
        return deComercio(c);
    }

    public ImpactoComercio deComercio(Comercio c) {
        Long id = c.getId();
        BigDecimal vendido = dec(em.createQuery("""
                select coalesce(sum(r.cantidad * o.pesoUnitarioKg), 0) from Reserva r, Oferta o
                where r.ofertaId = o.id and o.comercioId = :c and r.estado = 'RECOGIDA'""").setParameter("c", id).getSingleResult());
        BigDecimal donado = dec(em.createQuery("""
                select coalesce(sum(d.pesoKg), 0) from Donacion d, Oferta o
                where d.ofertaId = o.id and o.comercioId = :c and d.estado = 'COMPLETADA'""").setParameter("c", id).getSingleResult());
        BigDecimal perdido = dec(em.createQuery("""
                select coalesce(sum(o.cantidad * o.pesoUnitarioKg), 0) from Oferta o
                where o.comercioId = :c and o.estado = 'PERDIDA'""").setParameter("c", id).getSingleResult());
        BigDecimal ingresos = dec(em.createQuery("""
                select coalesce(sum(r.monto), 0) from Reserva r, Oferta o
                where r.ofertaId = o.id and o.comercioId = :c and r.estado in ('PAGADA','RECOGIDA')""").setParameter("c", id).getSingleResult());
        long donaciones = ((Number) em.createQuery("""
                select count(d) from Donacion d, Oferta o
                where d.ofertaId = o.id and o.comercioId = :c and d.estado = 'COMPLETADA'""").setParameter("c", id).getSingleResult()).longValue();

        BigDecimal rescatado = vendido.add(donado);
        BigDecimal avance = distintivoMinKg.signum() == 0 ? BigDecimal.valueOf(100)
                : rescatado.multiply(BigDecimal.valueOf(100)).divide(distintivoMinKg, 0, RoundingMode.DOWN).min(BigDecimal.valueOf(100));
        boolean premium = "PREMIUM".equals(c.getPlan());
        boolean alcanzado = rescatado.compareTo(distintivoMinKg) >= 0;
        String mensaje = !premium ? "El distintivo es parte del plan de suscripción."
                : alcanzado ? "¡Distintivo de impacto verificado otorgado!"
                : "Aún no alcanzas el mínimo para el distintivo de este período (" + rescatado.setScale(1, RoundingMode.HALF_UP)
                        + " de " + distintivoMinKg.stripTrailingZeros().toPlainString() + " kg).";
        return new ImpactoComercio(escalar(vendido), escalar(donado), escalar(rescatado), escalar(perdido),
                ingresos.setScale(2, RoundingMode.HALF_UP), escalar(rescatado.multiply(factorCo2)), donaciones,
                c.getPlan(), new Distintivo(premium && alcanzado, distintivoMinKg, avance, mensaje));
    }

    /** US20: panel de impacto agregado de toda la plataforma. */
    public ImpactoPlataforma plataforma() {
        BigDecimal vendido = dec(em.createQuery("""
                select coalesce(sum(r.cantidad * o.pesoUnitarioKg), 0) from Reserva r, Oferta o
                where r.ofertaId = o.id and r.estado = 'RECOGIDA'""").getSingleResult());
        BigDecimal donado = dec(em.createQuery("select coalesce(sum(d.pesoKg), 0) from Donacion d where d.estado = 'COMPLETADA'").getSingleResult());
        BigDecimal rescatado = vendido.add(donado);
        BigDecimal avance = metaPlataformaKg.signum() == 0 ? BigDecimal.ZERO
                : rescatado.multiply(BigDecimal.valueOf(100)).divide(metaPlataformaKg, 2, RoundingMode.HALF_UP);
        return new ImpactoPlataforma(escalar(rescatado), escalar(vendido), escalar(donado), escalar(rescatado.multiply(factorCo2)),
                comercios.countByEstado(EstadoCuenta.VALIDADA), organizaciones.countByEstado(EstadoCuenta.VALIDADA),
                metaPlataformaKg, avance);
    }

    public ImpactoOrganizacion deOrganizacion(Long usuarioId) {
        OrganizacionSocial org = organizaciones.findByUsuarioId(usuarioId).orElseThrow(() -> ExcepcionApi.noEncontrado("Organización no encontrada"));
        BigDecimal kg = dec(em.createQuery("select coalesce(sum(d.pesoKg), 0) from Donacion d where d.organizacionId = :o and d.estado = 'COMPLETADA'")
                .setParameter("o", org.getId()).getSingleResult());
        long n = ((Number) em.createQuery("select count(d) from Donacion d where d.organizacionId = :o and d.estado = 'COMPLETADA'")
                .setParameter("o", org.getId()).getSingleResult()).longValue();
        return new ImpactoOrganizacion(escalar(kg), n, escalar(kg.multiply(factorCo2)));
    }

    /** US16: reporte de impacto en CSV (solo plan PREMIUM). */
    public String reporteComercioCsv(Long usuarioId) {
        Comercio c = comercios.findByUsuarioId(usuarioId).orElseThrow(() -> ExcepcionApi.noEncontrado("Comercio no encontrado"));
        exigirPremium(c);
        ImpactoComercio i = deComercio(c);
        if (i.rescatadoKg().signum() == 0 && i.ingresos().signum() == 0) {
            throw ExcepcionApi.noEncontrado("No hay datos para el período seleccionado");
        }
        StringBuilder sb = new StringBuilder("﻿");   // BOM para que Excel abra bien los acentos
        sb.append("Reporte de impacto SobraCero\n");
        sb.append("Comercio,").append(csv(c.getNombre())).append('\n');
        sb.append("RUC,").append(c.getRuc()).append('\n');
        sb.append("Métrica,Valor\n");
        sb.append("Kilos vendidos y recogidos,").append(i.vendidoKg()).append('\n');
        sb.append("Kilos donados,").append(i.donadoKg()).append('\n');
        sb.append("Kilos rescatados (total),").append(i.rescatadoKg()).append('\n');
        sb.append("Kilos perdidos (no evitados),").append(i.perdidoKg()).append('\n');
        sb.append("CO2 evitado (kg),").append(i.co2EvitadoKg()).append('\n');
        sb.append("Ingresos recuperados (S/),").append(i.ingresos()).append('\n');
        sb.append("Donaciones completadas,").append(i.donacionesCompletadas()).append('\n');
        return sb.toString();
    }

    public void exigirPremium(Comercio c) {
        if (!"PREMIUM".equals(c.getPlan())) {
            throw new ExcepcionApi(HttpStatus.FORBIDDEN, "Renueva tu suscripción para acceder a este módulo");
        }
    }

    private static String csv(String s) {
        return '"' + s.replace("\"", "\"\"") + '"';
    }

    private static BigDecimal dec(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }

    private static BigDecimal escalar(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
