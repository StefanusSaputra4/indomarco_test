package com.indomaret.backend.config;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.indomaret.backend.controller.AuthController;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Klik Indomaret Backend API")
                        .description("Technical Assignment Backend Developer - Klik Indomaret")
                        .version("1.0"));
    }

    @Bean
    public OperationCustomizer customize() {
        return (operation, handlerMethod) -> {
            // Kecualikan endpoint login/auth agar tidak menampilkan header Authorization
            if (!handlerMethod.getBeanType().equals(AuthController.class)) {
                Parameter headerParam = new HeaderParameter()
                        .name("Authorization")
                        .description("Token JWT (Format: Bearer <token>)")
                        .required(false)
                        .schema(new StringSchema().example("Bearer eyJhbGciOi..."));
                operation.addParametersItem(headerParam);
            }
            return operation;
        };
    }
}

