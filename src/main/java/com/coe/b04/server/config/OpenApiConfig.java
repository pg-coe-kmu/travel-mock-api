package com.coe.b04.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * OpenAPI / Swagger configuration.
 * Exposes the JSON spec at /v3/api-docs and the Swagger UI at /swagger-ui.html
 * (both served by springdoc on every profile).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI travelMockOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Travel Mock API")
                        .description("Mock API for searching flights, hotels and rental cars.")
                        .version("0.0.1"));
    }
}
