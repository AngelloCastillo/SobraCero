package pe.upc.sobracero.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Seguridad sin sesión con JWT (HS256). El rol viaja en el claim "rol" y se convierte en ROLE_xxx. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SeguridadConfig {

    @Bean
    SecretKey claveJwt(@Value("${app.jwt.secreto}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secreto debe tener al menos 32 caracteres");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey claveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey claveJwt) {
        return NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("rol");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/autenticacion/login", "/api/autenticacion/registro").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ofertas/cercanas", "/api/ofertas/{id}", "/api/impacto/plataforma").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(convertidor)));
        return http.build();
    }

    @Bean
    PasswordEncoder codificadorClaves() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Orígenes permitidos para el frontend (lista separada por comas).
     * IMPORTANTE: el nombre del bean debe ser exactamente "corsConfigurationSource": Spring Security lo busca por ese nombre.
     */
    @Bean(name = "corsConfigurationSource")
    CorsConfigurationSource configuracionCors(@Value("${app.cors.origenes-permitidos}") List<String> origenes) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(origenes);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", cfg);
        return fuente;
    }
}
