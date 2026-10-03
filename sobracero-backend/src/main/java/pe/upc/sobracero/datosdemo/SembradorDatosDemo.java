package pe.upc.sobracero.datosdemo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.sobracero.comercios.Comercio;
import pe.upc.sobracero.comercios.ComercioRepository;
import pe.upc.sobracero.comun.EstadoCuenta;
import pe.upc.sobracero.comun.GeneradorCodigo;
import pe.upc.sobracero.comun.Rol;
import pe.upc.sobracero.donaciones.Donacion;
import pe.upc.sobracero.donaciones.DonacionRepository;
import pe.upc.sobracero.ofertas.Oferta;
import pe.upc.sobracero.ofertas.OfertaRepository;
import pe.upc.sobracero.organizaciones.OrganizacionSocial;
import pe.upc.sobracero.organizaciones.OrganizacionSocialRepository;
import pe.upc.sobracero.reservas.Reserva;
import pe.upc.sobracero.reservas.ReservaRepository;
import pe.upc.sobracero.usuarios.Usuario;
import pe.upc.sobracero.usuarios.UsuarioRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Datos de demostración (app.datos-demo=true): comercios, organizaciones, ofertas de hoy e historial de 20 días
 * para que el panel de impacto y la predicción muestren información real en la presentación.
 * Todas las cuentas demo usan la contraseña "Demo1234".
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.datos-demo", havingValue = "true")
public class SembradorDatosDemo implements ApplicationRunner {

    private static final String CLAVE = "Demo1234";

    private final UsuarioRepository usuarios;
    private final ComercioRepository comercios;
    private final OrganizacionSocialRepository organizaciones;
    private final OfertaRepository ofertas;
    private final ReservaRepository reservas;
    private final DonacionRepository donaciones;
    private final PasswordEncoder codificador;
    private final Clock reloj;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.existsByCorreoIgnoreCase("consumidor@demo.pe")) return;

        Usuario consumidor = usuario("consumidor@demo.pe", Rol.CONSUMIDOR, "Camila Rojas");

        Comercio rosa = comercio("comercio1@demo.pe", "Panadería Doña Rosa", "20501234561", "Av. La Marina 2355", "San Miguel", -12.0762, -77.0845, EstadoCuenta.VALIDADA, "PREMIUM");
        Comercio sabor = comercio("comercio2@demo.pe", "Restaurante El Sabor", "20501234562", "Jr. Ayacucho 410", "San Miguel", -12.0785, -77.0801, EstadoCuenta.VALIDADA, "GRATIS");
        Comercio andes = comercio("comercio3@demo.pe", "Minimarket Los Andes", "20501234563", "Av. Universitaria 1801", "San Miguel", -12.0741, -77.0862, EstadoCuenta.VALIDADA, "GRATIS");
        Comercio santaRosa = comercio("comercio4@demo.pe", "Bodega Santa Rosa", "20501234564", "Calle Los Cedros 155", "Magdalena", -12.0810, -77.0790, EstadoCuenta.VALIDADA, "GRATIS");
        Comercio lucia = comercio("comercio5@demo.pe", "Pastelería Lucía", "20501234565", "Av. Brasil 3120", "Magdalena", -12.0725, -77.0780, EstadoCuenta.VALIDADA, "GRATIS");
        comercio("comercio6@demo.pe", "Cafetería Central", "20501234566", "Av. Costanera 800", "San Miguel", -12.0700, -77.0900, EstadoCuenta.PENDIENTE, "GRATIS");

        OrganizacionSocial esperanza = organizacion("ong1@demo.pe", "Olla Común Nueva Esperanza", "Av. Pachacútec 1200, Villa El Salvador", -12.2130, -76.9370, new BigDecimal("40"), EstadoCuenta.VALIDADA, true);
        organizacion("ong2@demo.pe", "Comedor Popular San Juan", "Av. Los Héroes 340, San Juan de Miraflores", -12.1550, -76.9700, new BigDecimal("25"), EstadoCuenta.VALIDADA, false);
        organizacion("ong3@demo.pe", "Albergue Manos Solidarias", "Jr. Tacna 210, Rímac", -12.0300, -77.0400, null, EstadoCuenta.PENDIENTE, false);

        // Historial de 20 días para "Doña Rosa" (impacto y predicción)
        String[] categorias = {"Panadería", "Panadería", "Comida preparada", "Lácteos"};
        LocalDate hoy = LocalDate.now(reloj);
        for (int i = 20; i >= 1; i--) {
            LocalDate dia = hoy.minusDays(i);
            String categoria = categorias[i % categorias.length];
            int inicial = 10, vendidas = 3 + (i * 7) % 6;
            int sobraron = inicial - vendidas;
            Oferta o = oferta(rosa, categoria.equals("Panadería") ? "Pan francés (bolsa x10)" : categoria.equals("Lácteos") ? "Yogurt natural 1L" : "Menú del día",
                    categoria, sobraron, inicial, "0.6", "10.00", "4.00", dia, dia.atTime(19, 0), dia.atTime(21, 0));
            o.setEstado(sobraron == 0 ? Oferta.AGOTADA : (i % 3 == 0 ? Oferta.PERDIDA : Oferta.DONADA));
            ofertas.save(o);
            for (int k = 0; k < vendidas; k++) {
                Reserva r = new Reserva();
                r.setOfertaId(o.getId());
                r.setConsumidorId(consumidor.getId());
                r.setCodigo(GeneradorCodigo.generar("SC-", 6));
                r.setMonto(new BigDecimal("4.00"));
                r.setEstado(Reserva.RECOGIDA);
                r.setRecogidaEn(dia.atTime(20, 0));
                reservas.save(r);
            }
            if (o.getEstado().equals(Oferta.DONADA)) {
                Donacion d = new Donacion();
                d.setOfertaId(o.getId());
                d.setOrganizacionId(esperanza.getId());
                d.setCodigo(GeneradorCodigo.generar("DON-", 6));
                d.setCantidad(sobraron);
                d.setPesoKg(new BigDecimal("0.6").multiply(BigDecimal.valueOf(sobraron)));
                d.setEstado(Donacion.COMPLETADA);
                d.setCompletadaEn(dia.atTime(21, 10));
                donaciones.save(d);
            }
        }

        // Ofertas activas de hoy (ventana abierta hasta la noche)
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime fin = ahora.plusHours(6);
        LocalDate manana = hoy.plusDays(1);
        oferta(rosa, "Pan francés (bolsa x10)", "Panadería", 8, 8, "0.6", "10.00", "4.00", manana, ahora.minusHours(1), fin);
        oferta(rosa, "Pack de empanadas x6", "Panadería", 5, 5, "0.6", "15.00", "7.00", hoy, ahora.minusHours(1), fin);
        oferta(sabor, "Menú del día (almuerzo)", "Comida preparada", 5, 5, "0.5", "16.00", "7.00", hoy, ahora.minusHours(1), fin);
        oferta(andes, "Yogurt natural 1L", "Lácteos", 12, 12, "1.0", "8.50", "4.50", manana, ahora.minusHours(1), fin);
        oferta(santaRosa, "Frutas mixtas (2 kg)", "Frutas y verduras", 6, 6, "2.0", "12.00", "6.00", manana, ahora.minusHours(1), fin);
        oferta(lucia, "Pack de croissants x6", "Panadería", 4, 4, "0.4", "18.00", "8.00", hoy, ahora.minusHours(1), fin);
        log.info("Datos de demostración creados (contraseña de todas las cuentas demo: {})", CLAVE);
    }

    private Usuario usuario(String correo, Rol rol, String nombre) {
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setClaveHash(codificador.encode(CLAVE));
        u.setRol(rol);
        u.setNombreMostrado(nombre);
        return usuarios.save(u);
    }

    private Comercio comercio(String correo, String nombre, String ruc, String direccion, String distrito,
                              double lat, double lng, EstadoCuenta estado, String plan) {
        Usuario u = usuario(correo, Rol.COMERCIO, nombre);
        Comercio c = new Comercio();
        c.setUsuarioId(u.getId());
        c.setNombre(nombre);
        c.setRuc(ruc);
        c.setDireccion(direccion);
        c.setDistrito(distrito);
        c.setLatitud(lat);
        c.setLongitud(lng);
        c.setEstado(estado);
        c.setPlan(plan);
        return comercios.save(c);
    }

    private OrganizacionSocial organizacion(String correo, String nombre, String direccion, double lat, double lng,
                                            BigDecimal capacidad, EstadoCuenta estado, boolean sunat) {
        Usuario u = usuario(correo, Rol.ORGANIZACION, nombre);
        OrganizacionSocial o = new OrganizacionSocial();
        o.setUsuarioId(u.getId());
        o.setNombre(nombre);
        o.setDireccion(direccion);
        o.setLatitud(lat);
        o.setLongitud(lng);
        o.setCapacidadDiariaKg(capacidad);
        o.setEstado(estado);
        o.setDeducibleSunat(sunat);
        return organizaciones.save(o);
    }

    private Oferta oferta(Comercio c, String titulo, String categoria, int cantidad, int inicial, String peso,
                          String original, String descuento, LocalDate vence, LocalDateTime desde, LocalDateTime hasta) {
        Oferta o = new Oferta();
        o.setComercioId(c.getId());
        o.setTitulo(titulo);
        o.setCategoria(categoria);
        o.setCantidad(cantidad);
        o.setCantidadInicial(inicial);
        o.setPesoUnitarioKg(new BigDecimal(peso));
        o.setPrecioOriginal(new BigDecimal(original));
        o.setPrecioDescuento(new BigDecimal(descuento));
        o.setVenceEl(vence);
        o.setRecojoDesde(desde);
        o.setRecojoHasta(hasta);
        return ofertas.save(o);
    }
}
