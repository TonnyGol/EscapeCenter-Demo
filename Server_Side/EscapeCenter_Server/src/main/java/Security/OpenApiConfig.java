package Security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Adds the Authorize button to Swagger UI, so staff endpoints can be tried with the admin login.
@Configuration
public class OpenApiConfig {
    private static final String BASIC_AUTH_SCHEME = "basicAuth";

    @Bean
    public OpenAPI escapeCenterOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BASIC_AUTH_SCHEME,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME));
    }
}
