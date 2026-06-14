package com.backend.security.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Backend API Documentation")
                        .version("1.0.0"))
                .addSecurityItem(
                        new SecurityRequirement().addList(SECURITY_SCHEME_NAME)
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_NAME,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                );
    }

    @Bean
    public OpenApiCustomizer regexExampleCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null
                    || openApi.getComponents().getSchemas() == null) {
                return;
            }

            openApi.getComponents()
                    .getSchemas()
                    .values()
                    .forEach(this::processSchema);
        };
    }

    private void processSchema(Schema<?> schema) {
        if (schema.getProperties() == null) {
            return;
        }

        schema.getProperties().values().forEach(property -> {
            if (property instanceof Schema<?> propertySchema) {

                boolean hasPattern = propertySchema.getPattern() != null;
                boolean hasExample = propertySchema.getExample() != null;

                if (hasPattern && !hasExample) {
                    propertySchema.setExample("string");
                }
            }
        });
    }
}