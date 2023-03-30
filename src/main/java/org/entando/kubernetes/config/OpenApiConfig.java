package org.entando.kubernetes.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class OpenApiConfig {

    private final String moduleName;
    private final String apiVersion;

    public OpenApiConfig(
            @Value("${module-name:component-manager}") String moduleName,
            @Value("${api-version:v1}") String apiVersion) {
        this.moduleName = moduleName;
        this.apiVersion = apiVersion;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        final String apiTitle = String.format("%s API", StringUtils.capitalize(moduleName));
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                                .addSchemas("HubRegistryResponseSchema", getHubRegistryResponseSchema())
                )
                .info(new Info().title(apiTitle).version(apiVersion));
    }

    private Schema getHubRegistryResponseSchema() {
        return new Schema<Map<String, String>>()
                .addProperties("id",new StringSchema().example("string"))
                .addProperties("name",new StringSchema().example("string"))
                .addProperties("url",new StringSchema().example("string"));
    }
}

