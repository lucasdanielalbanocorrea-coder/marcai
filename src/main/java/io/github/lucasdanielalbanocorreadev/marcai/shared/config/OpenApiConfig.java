package io.github.lucasdanielalbanocorreadev.marcai.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI em /swagger-ui.html — o botão "Authorize" obtém o token via client_credentials. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String ESQUEMA = "oauth2";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Marcai API")
                        .description("Agendamentos com lembretes e confirmação pelo WhatsApp")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows().clientCredentials(new OAuthFlow().tokenUrl("/oauth2/token")))))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
