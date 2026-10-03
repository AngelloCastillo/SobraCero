package pe.upc.sobracero;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pe.upc.sobracero.donaciones.CicloVidaService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Recorre el flujo completo del producto contra un PostgreSQL real (embebido, sin Docker). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlujoCompletoIntegracionTest {

    static final EmbeddedPostgres PG;
    static {
        try { PG = EmbeddedPostgres.builder().start(); } catch (IOException e) { throw new RuntimeException(e); }
    }

    static final RelojModificable RELOJ = new RelojModificable(ZoneId.of("America/Lima"), LocalDateTime.of(2026, 9, 29, 18, 0));

    @TestConfiguration
    static class RelojDePruebas {
        @Bean @Primary RelojModificable relojDePruebas() { return RELOJ; }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> PG.getJdbcUrl("postgres", "postgres"));
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "postgres");
    }

    @LocalServerPort int puerto;
    @Autowired JsonMapper json;
    @Autowired CicloVidaService cicloVida;

    final HttpClient http = HttpClient.newHttpClient();

    record Resp(int estado, JsonNode cuerpo) {}

    // estado compartido entre pasos ordenados
    static String tokenAdmin, tokenConsumidor, tokenComercio, tokenOng1, tokenOng2;
    static long comercioId, ong1Id, ong2Id, ofertaAId, ofertaBId;
    static String codigoReserva;

    Resp llamar(String metodo, String ruta, String token, Object cuerpo) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Content-Type", "application/json");
        if (token != null) b.header("Authorization", "Bearer " + token);
        b.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(cuerpo)));
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode nodo = r.body() == null || r.body().isBlank() || r.body().startsWith("﻿") ? null : json.readTree(r.body());
        return new Resp(r.statusCode(), nodo);
    }

    String registrar(String correo, String rol, String nombre, Map<String, Object> extra) throws Exception {
        var m = new HashMap<String, Object>(extra);
        m.put("correo", correo); m.put("clave", "Passw0rd!"); m.put("rol", rol); m.put("nombre", nombre);
        Resp r = llamar("POST", "/api/autenticacion/registro", null, m);
        assertThat(r.estado()).as("registro " + correo + " -> " + r.cuerpo()).isEqualTo(201);
        return r.cuerpo().get("token").asString();
    }

    Map<String, Object> oferta(String titulo, int cantidad, double peso, LocalDateTime desde, LocalDateTime hasta, String vence) {
        var m = new HashMap<String, Object>();
        m.put("titulo", titulo); m.put("categoria", "Panadería"); m.put("cantidad", cantidad); m.put("pesoUnitarioKg", peso);
        m.put("precioOriginal", 10.0); m.put("precioDescuento", 4.0);
        m.put("recojoDesde", desde.toString()); m.put("recojoHasta", hasta.toString());
        if (vence != null) m.put("venceEl", vence);
        return m;
    }

    @Test @Order(1)
    void registroYRoles() throws Exception {
        Resp login = llamar("POST", "/api/autenticacion/login", null, Map.of("correo", "admin@test.pe", "clave", "Admin1234"));
        assertThat(login.estado()).isEqualTo(200);
        tokenAdmin = login.cuerpo().get("token").asString();

        tokenConsumidor = registrar("cami@test.pe", "CONSUMIDOR", "Camila", Map.of());
        tokenComercio = registrar("rosa@test.pe", "COMERCIO", "Panadería Rosa",
                Map.of("ruc", "20123456789", "direccion", "Av. La Marina 1", "latitud", -12.077, "longitud", -77.083));
        tokenOng1 = registrar("ves@test.pe", "ORGANIZACION", "Olla VES",
                Map.of("direccion", "VES", "latitud", -12.213, "longitud", -76.937, "capacidadDiariaKg", 40));
        tokenOng2 = registrar("rimac@test.pe", "ORGANIZACION", "Comedor Rímac",
                Map.of("direccion", "Rímac", "latitud", -12.03, "longitud", -77.04, "capacidadDiariaKg", 40));

        assertThat(llamar("POST", "/api/autenticacion/registro", null, Map.of("correo", "cami@test.pe", "clave", "Passw0rd!", "rol", "CONSUMIDOR", "nombre", "x")).estado()).isEqualTo(409);
        assertThat(llamar("POST", "/api/autenticacion/login", null, Map.of("correo", "cami@test.pe", "clave", "mala")).estado()).isEqualTo(401);
        assertThat(llamar("GET", "/api/reservas/mias", null, null).estado()).isEqualTo(401);
        assertThat(llamar("GET", "/api/reservas/mias", tokenComercio, null).estado()).isEqualTo(403);

        // Comercio sin validar no puede publicar (US18)
        var ahora = RELOJ.instant().atZone(ZoneId.of("America/Lima")).toLocalDateTime();
        Resp r = llamar("POST", "/api/ofertas", tokenComercio, oferta("Pan", 5, 1.0, ahora, ahora.plusHours(3), "2026-09-30"));
        assertThat(r.estado()).isEqualTo(403);
        assertThat(llamar("PATCH", "/api/comercios/1/validar", tokenConsumidor, null).estado()).isEqualTo(403);

        comercioId = llamar("GET", "/api/comercios/yo", tokenComercio, null).cuerpo().get("id").asLong();
        ong1Id = llamar("GET", "/api/organizaciones/yo", tokenOng1, null).cuerpo().get("id").asLong();
        ong2Id = llamar("GET", "/api/organizaciones/yo", tokenOng2, null).cuerpo().get("id").asLong();
        assertThat(llamar("PATCH", "/api/comercios/" + comercioId + "/validar", tokenAdmin, null).estado()).isEqualTo(200);
        assertThat(llamar("PATCH", "/api/organizaciones/" + ong1Id + "/validar", tokenAdmin, Map.of("deducibleSunat", true)).estado()).isEqualTo(200);
        assertThat(llamar("PATCH", "/api/organizaciones/" + ong2Id + "/validar", tokenAdmin, Map.of("deducibleSunat", false)).estado()).isEqualTo(200);
    }

    @Test @Order(2)
    void publicarBuscarReservarYRecoger() throws Exception {
        LocalDateTime desde = LocalDateTime.of(2026, 9, 29, 18, 0), hasta = LocalDateTime.of(2026, 9, 29, 21, 0);

        // US02/US03: validaciones
        Resp sinFecha = llamar("POST", "/api/ofertas", tokenComercio, oferta("Pan", 5, 1.0, desde, hasta, null));
        assertThat(sinFecha.estado()).isEqualTo(400);
        assertThat(sinFecha.cuerpo().get("detail").asString()).contains("Completa la fecha de vencimiento para publicar");
        Resp ventana = llamar("POST", "/api/ofertas", tokenComercio, oferta("Pan", 5, 1.0, hasta, desde, "2026-09-30"));
        assertThat(ventana.cuerpo().get("detail").asString()).contains("La hora límite debe ser posterior a la hora de inicio");

        Resp ok = llamar("POST", "/api/ofertas", tokenComercio, oferta("Pan francés", 5, 1.0, desde, hasta, "2026-09-30"));
        assertThat(ok.estado()).isEqualTo(201);
        ofertaAId = ok.cuerpo().get("id").asLong();

        // US06: búsqueda pública con distancia
        Resp cerca = llamar("GET", "/api/ofertas/cercanas?lat=-12.077&lng=-77.083&radioKm=1", null, null);
        assertThat(cerca.estado()).isEqualTo(200);
        assertThat(cerca.cuerpo().size()).isEqualTo(1);
        assertThat(cerca.cuerpo().get(0).get("comercioNombre").asString()).isEqualTo("Panadería Rosa");
        assertThat(llamar("GET", "/api/ofertas/cercanas?lat=-12.5&lng=-77.5&radioKm=1", null, null).cuerpo().size()).isZero();

        // US08: pago rechazado no descuenta stock
        Resp rechazo = llamar("POST", "/api/reservas", tokenConsumidor, Map.of("ofertaId", ofertaAId, "cantidad", 2, "metodoPago", "TARJETA_RECHAZADA"));
        assertThat(rechazo.estado()).isEqualTo(402);
        assertThat(rechazo.cuerpo().get("detail").asString()).contains("Pago rechazado");
        assertThat(llamar("GET", "/api/ofertas/" + ofertaAId, null, null).cuerpo().get("cantidad").asInt()).isEqualTo(5);

        Resp reserva = llamar("POST", "/api/reservas", tokenConsumidor, Map.of("ofertaId", ofertaAId, "cantidad", 2, "metodoPago", "YAPE"));
        assertThat(reserva.estado()).isEqualTo(201);
        codigoReserva = reserva.cuerpo().get("codigo").asString();
        assertThat(llamar("GET", "/api/ofertas/" + ofertaAId, null, null).cuerpo().get("cantidad").asInt()).isEqualTo(3);
        assertThat(llamar("POST", "/api/reservas", tokenConsumidor, Map.of("ofertaId", ofertaAId, "cantidad", 10, "metodoPago", "YAPE")).estado()).isEqualTo(409);

        // US09: canje de código
        assertThat(llamar("POST", "/api/reservas/canjear", tokenComercio, Map.of("codigo", codigoReserva)).estado()).isEqualTo(200);
        Resp usado = llamar("POST", "/api/reservas/canjear", tokenComercio, Map.of("codigo", codigoReserva));
        assertThat(usado.estado()).isEqualTo(409);
        assertThat(usado.cuerpo().get("detail").asString()).contains("Este código ya fue utilizado");
        assertThat(llamar("GET", "/api/notificaciones", tokenComercio, null).cuerpo().get("sinLeer").asInt()).isPositive();
    }

    @Test @Order(3)
    void derivacionAutomaticaADonacionYRegistro() throws Exception {
        RELOJ.fijar(LocalDateTime.of(2026, 9, 29, 20, 50));   // faltan 10 min para el cierre
        cicloVida.ejecutar();

        // ong2 (Rímac) es la más cercana al comercio de San Miguel
        Resp d2 = llamar("GET", "/api/donaciones/mias", tokenOng2, null);
        assertThat(d2.cuerpo().size()).isEqualTo(1);
        assertThat(d2.cuerpo().get(0).get("estado").asString()).isEqualTo("OFRECIDA");
        assertThat(d2.cuerpo().get(0).get("pesoKg").asDouble()).isEqualTo(3.0);
        assertThat(llamar("GET", "/api/donaciones/mias", tokenOng1, null).cuerpo().size()).isZero();
        assertThat(llamar("GET", "/api/notificaciones", tokenOng2, null).cuerpo().get("sinLeer").asInt()).isPositive();

        long donacionId = d2.cuerpo().get(0).get("id").asLong();
        String codigo = d2.cuerpo().get(0).get("codigo").asString();
        assertThat(llamar("POST", "/api/donaciones/" + donacionId + "/aceptar", tokenOng2, null).cuerpo().get("estado").asString()).isEqualTo("ACEPTADA");

        Resp malo = llamar("POST", "/api/donaciones/completar", tokenOng2, Map.of("codigo", "DON-XXXXX"));
        assertThat(malo.estado()).isEqualTo(400);
        assertThat(malo.cuerpo().get("detail").asString()).contains("Código no válido para esta donación");
        assertThat(llamar("POST", "/api/donaciones/completar", tokenOng1, Map.of("codigo", codigo)).estado()).isEqualTo(400);  // no es de su organización
        assertThat(llamar("POST", "/api/donaciones/completar", tokenOng2, Map.of("codigo", codigo)).cuerpo().get("estado").asString()).isEqualTo("COMPLETADA");

        Resp reg = llamar("GET", "/api/donaciones/registro", tokenOng2, null);
        assertThat(reg.cuerpo().size()).isEqualTo(1);
        assertThat(reg.cuerpo().get(0).get("donante").asString()).isEqualTo("Panadería Rosa");

        // Impacto (US05, US20)
        Resp imp = llamar("GET", "/api/impacto/comercio", tokenComercio, null);
        assertThat(imp.cuerpo().get("vendidoKg").asDouble()).isEqualTo(2.0);
        assertThat(imp.cuerpo().get("donadoKg").asDouble()).isEqualTo(3.0);
        assertThat(imp.cuerpo().get("rescatadoKg").asDouble()).isEqualTo(5.0);
        assertThat(imp.cuerpo().get("ingresos").asDouble()).isEqualTo(8.0);
        assertThat(imp.cuerpo().get("co2EvitadoKg").asDouble()).isEqualTo(12.5);
        assertThat(llamar("GET", "/api/impacto/plataforma", null, null).cuerpo().get("rescatadoKg").asDouble()).isEqualTo(5.0);
    }

    @Test @Order(4)
    void rechazoRedirigeYSinOrganizacionQuedaComoPerdida() throws Exception {
        RELOJ.fijar(LocalDateTime.of(2026, 9, 30, 18, 0));
        LocalDateTime desde = LocalDateTime.of(2026, 9, 30, 18, 0), hasta = LocalDateTime.of(2026, 9, 30, 21, 0);
        ofertaBId = llamar("POST", "/api/ofertas", tokenComercio, oferta("Croissants", 4, 1.0, desde, hasta, "2026-10-01")).cuerpo().get("id").asLong();
        Resp reserva = llamar("POST", "/api/reservas", tokenConsumidor, Map.of("ofertaId", ofertaBId, "cantidad", 1, "metodoPago", "PLIN"));
        assertThat(reserva.estado()).isEqualTo(201);

        // US10: recordatorio a 30 min del cierre
        RELOJ.fijar(LocalDateTime.of(2026, 9, 30, 20, 35));
        cicloVida.ejecutar();
        var notas = llamar("GET", "/api/notificaciones", tokenConsumidor, null).cuerpo().get("items");
        boolean recordatorio = false;
        for (JsonNode n : notas) recordatorio |= n.get("titulo").asString().contains("recojo cierra pronto");
        assertThat(recordatorio).isTrue();

        // Derivación → ong2 rechaza → pasa a ong1 → ong1 rechaza → pérdida no evitada
        RELOJ.fijar(LocalDateTime.of(2026, 9, 30, 20, 50));
        cicloVida.ejecutar();
        var d2 = llamar("GET", "/api/donaciones/mias", tokenOng2, null).cuerpo().get(0);
        assertThat(d2.get("estado").asString()).isEqualTo("OFRECIDA");
        assertThat(llamar("POST", "/api/donaciones/" + d2.get("id").asLong() + "/rechazar", tokenOng2, null).estado()).isEqualTo(200);
        var d1 = llamar("GET", "/api/donaciones/mias", tokenOng1, null).cuerpo().get(0);
        assertThat(d1.get("estado").asString()).isEqualTo("OFRECIDA");
        assertThat(llamar("POST", "/api/donaciones/" + d1.get("id").asLong() + "/rechazar", tokenOng1, null).estado()).isEqualTo(200);

        var mias = llamar("GET", "/api/ofertas/mias", tokenComercio, null).cuerpo();
        String estadoB = null;
        for (JsonNode o : mias) if (o.get("id").asLong() == ofertaBId) estadoB = o.get("estado").asString();
        assertThat(estadoB).isEqualTo("PERDIDA");
        boolean aviso = false;
        for (JsonNode n : llamar("GET", "/api/notificaciones", tokenComercio, null).cuerpo().get("items")) {
            aviso |= n.get("mensaje").asString().contains("No se encontró una organización disponible");
        }
        assertThat(aviso).isTrue();

        // Reserva no recogida
        RELOJ.fijar(LocalDateTime.of(2026, 9, 30, 21, 5));
        cicloVida.ejecutar();
        assertThat(llamar("GET", "/api/reservas/mias", tokenConsumidor, null).cuerpo().get(0).get("estado").asString()).isEqualTo("NO_RECOGIDA");
    }

    @Test @Order(5)
    void planPremiumReportesYPrediccion() throws Exception {
        Resp bloqueado = llamar("GET", "/api/prediccion/comercio", tokenComercio, null);
        assertThat(bloqueado.estado()).isEqualTo(403);
        assertThat(bloqueado.cuerpo().get("detail").asString()).contains("Renueva tu suscripción");

        assertThat(llamar("PATCH", "/api/comercios/" + comercioId + "/plan", tokenAdmin, Map.of("plan", "PREMIUM")).estado()).isEqualTo(200);
        Resp pred = llamar("GET", "/api/prediccion/comercio", tokenComercio, null);
        assertThat(pred.estado()).isEqualTo(200);
        assertThat(pred.cuerpo().get("disponible").asBoolean()).isFalse();
        assertThat(pred.cuerpo().get("mensaje").asString()).contains("Aún no hay suficiente historial para predecir");

        HttpRequest csv = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/impacto/comercio/reporte.csv"))
                .header("Authorization", "Bearer " + tokenComercio).GET().build();
        HttpResponse<String> r = http.send(csv, HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("Kilos rescatados");
        assertThat(llamar("GET", "/api/impacto/comercio", tokenComercio, null).cuerpo().get("plan").asString()).isEqualTo("PREMIUM");
    }

    @Test @Order(6)
    void agotarTodasLasUnidadesNoViolaRestricciones() throws Exception {
        // Regresión: reservar todas las unidades deja cantidad = 0 y la oferta pasa a AGOTADA
        RELOJ.fijar(LocalDateTime.of(2026, 10, 2, 18, 0));
        LocalDateTime desde = LocalDateTime.of(2026, 10, 2, 18, 0), hasta = LocalDateTime.of(2026, 10, 2, 21, 0);
        long id = llamar("POST", "/api/ofertas", tokenComercio, oferta("Última bolsa", 1, 1.0, desde, hasta, "2026-10-03")).cuerpo().get("id").asLong();
        assertThat(llamar("POST", "/api/reservas", tokenConsumidor, Map.of("ofertaId", id, "cantidad", 1, "metodoPago", "YAPE")).estado()).isEqualTo(201);
        assertThat(llamar("POST", "/api/reservas", tokenConsumidor, Map.of("ofertaId", id, "cantidad", 1, "metodoPago", "YAPE")).estado()).isEqualTo(409);
        var mias = llamar("GET", "/api/ofertas/mias", tokenComercio, null).cuerpo();
        String estado = null;
        for (JsonNode o : mias) if (o.get("id").asLong() == id) estado = o.get("estado").asString();
        assertThat(estado).isEqualTo("AGOTADA");
    }
}
