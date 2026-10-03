package pe.upc.sobracero;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.SpringApplication;

/**
 * Arranca el backend con un PostgreSQL embebido (sin instalar nada, sin Docker) y datos de demostración.
 * Útil para probar rápido. En IntelliJ: clic derecho → Run 'LocalEmbeddedApplication.main()'.
 * Los datos se pierden al detenerlo. Puerto de la API: 8080.
 */
public class LocalEmbeddedApplication {

    public static void main(String[] args) throws Exception {
        EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { pg.close(); } catch (Exception ignored) { }
        }));
        String[] all = new String[args.length + 3];
        System.arraycopy(args, 0, all, 3, args.length);
        all[0] = "--spring.datasource.url=" + pg.getJdbcUrl("postgres", "postgres");
        all[1] = "--spring.datasource.username=postgres";
        all[2] = "--spring.datasource.password=postgres";
        SpringApplication.run(SobraceroBackendApplication.class, all);
    }
}
