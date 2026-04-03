package com.medicalstorage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registers the OpenAPI 3.0 specification served at /api-docs.
 * Swagger UI is automatically enabled by springdoc-openapi.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8085}")
    private String serverPort;

    @Bean
    public OpenAPI medicalStorageOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Medical Image Storage API")
                        .description("REST API for storing and retrieving medical images (MRI/CT/X-Ray) " +
                                     "using SeaweedFS distributed object storage.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Healthcare IT Team")
                                .email("dev@medicalstorage.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")
                ));
    }
}
