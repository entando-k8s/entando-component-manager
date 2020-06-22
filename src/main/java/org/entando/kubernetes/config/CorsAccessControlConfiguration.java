package org.entando.kubernetes.config;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@EnableWebMvc
@Configuration
@ConfigurationProperties(prefix = "cors.access.control")
@Setter
public class CorsAccessControlConfiguration implements WebMvcConfigurer {

    private List<String> origin = Arrays.asList("*");
    private List<String> headers = Arrays.asList("Content-Type", "Authorization");
    private List<String> methods = Arrays.asList("GET", "POST", "DELETE", "OPTIONS", "PATCH");
    long maxAge = 3600;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origin.toArray(new String[0]))
                .allowedHeaders(headers.toArray(new String[0]))
                .allowedMethods(methods.toArray(new String[0]))
                .maxAge(maxAge)
                .allowCredentials(true);

    }
}
