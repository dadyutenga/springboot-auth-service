package com.example.dada.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Create the OpenAPI model for the Boda Boda Delivery System with API metadata and JWT bearer security.
     *
     * @return the configured OpenAPI instance containing title, description, version, contact information, a security requirement,
     *         and a HTTP Bearer security scheme named "bearerAuth" that uses the JWT format
     */
    @Bean
    public OpenAPI bodaBodaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Boda Boda Delivery System API")
                        .description("Production-ready APIs for riders, customers, and administrators")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Boda Boda Support")
                                .email("support@example.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}