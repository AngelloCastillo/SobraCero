package pe.upc.sobracero;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica que los datos de demostración se crean y que las cuentas demo funcionan. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.datos-demo=true")
@ActiveProfiles("test")
class DatosDemoTest {

    static final EmbeddedPostgres PG;
    static {
        try { PG = EmbeddedPostgres.builder().start(); } catch (IOException e) { throw new RuntimeException(e); }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> PG.getJdbcUrl("postgres", "postgres"));
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "postgres");
    }

    @LocalServerPort int puerto;
    @Autowired JsonMapper json;
    final HttpClient http = HttpClient.newHttpClient();

    JsonNode obtener(String ruta, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
        if (token != null) b.header("Authorization", "Bearer " + token);
        HttpResponse<String> r = http.send(b.GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).as(ruta + " -> " + r.body()).isEqualTo(200);
        return json.readTree(r.body());
    }

    String login(String correo) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/autenticacion/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"correo\":\"" + correo + "\",\"clave\":\"Demo1234\"}")).build();
        HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).as(correo).isEqualTo(200);
        return json.readTree(r.body()).get("token").asString();
    }

    @Test
    void corsPermiteAlFrontendYBloqueaOtrosOrigenes() throws Exception {
        String url = "http://localhost:" + puerto + "/api/ofertas/cercanas?lat=-12.077&lng=-77.083&radioKm=1";
        HttpResponse<String> permitido = http.send(HttpRequest.newBuilder(URI.create(url)).header("Origin", "http://localhost:4200").GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(permitido.statusCode()).isEqualTo(200);
        assertThat(permitido.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:4200");

        HttpResponse<String> bloqueado = http.send(HttpRequest.newBuilder(URI.create(url)).header("Origin", "http://sitio-malicioso.com").GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(bloqueado.statusCode()).isEqualTo(403);

        // Preflight del login desde el frontend
        HttpResponse<String> preflight = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/autenticacion/login"))
                .header("Origin", "http://localhost:4200").header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:4200");
    }

    @Test
    void datosDemoDisponibles() throws Exception {
        // ofertas activas cerca de San Miguel
        assertThat(obtener("/api/ofertas/cercanas?lat=-12.077&lng=-77.083&radioKm=5", null).size()).isGreaterThanOrEqualTo(6);

        String rosa = login("comercio1@demo.pe");
        JsonNode impacto = obtener("/api/impacto/comercio", rosa);
        assertThat(impacto.get("rescatadoKg").asDouble()).isPositive();
        assertThat(impacto.get("distintivo").get("otorgado").asBoolean()).isTrue();
        JsonNode prediccion = obtener("/api/prediccion/comercio", rosa);
        assertThat(prediccion.get("disponible").asBoolean()).isTrue();
        assertThat(prediccion.get("predicciones").size()).isPositive();

        String consumidor = login("consumidor@demo.pe");
        assertThat(obtener("/api/reservas/mias", consumidor).isArray()).isTrue();
        assertThat(obtener("/api/impacto/plataforma", null).get("rescatadoKg").asDouble()).isPositive();
    }
}
