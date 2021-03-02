package org.entando.kubernetes.config;

import java.util.Arrays;
import java.util.List;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConfigurationProperties(prefix = "cors.access.control.allow")
@Setter
public class CorsAccessControlConfiguration implements WebMvcConfigurer {

    private List<String> origins = Arrays.asList("*");
    private List<String> headers = Arrays.asList("Content-Type", "Authorization");
    private List<String> methods = Arrays.asList("GET", "POST", "DELETE", "OPTIONS", "PATCH");
    private boolean credentials = true;

    @Value("${cors.access.control.maxAge:3600}")
    private long maxAge;

    @Value("${cors.enabled:false}")
    private boolean isCorsEnabled;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (isCorsEnabled) {
            registry.addMapping("/**")
                    .allowedOriginPatterns(origins.toArray(new String[0]))
                    .allowedHeaders(headers.toArray(new String[0]))
                    .allowedMethods(methods.toArray(new String[0]))
                    .maxAge(maxAge)
                    .allowCredentials(credentials);
        }
    }
}
