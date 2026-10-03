package pe.upc.sobracero.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentación Swagger con botón "Authorize" para pegar el token JWT. */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "bearerAuth";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("SobraCero API").version("1.0")
                        .description("Marketplace de rescate de alimentos: ventas con descuento y donaciones automáticas."))
                .components(new Components().addSecuritySchemes(ESQUEMA,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
