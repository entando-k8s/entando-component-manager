package org.entando.kubernetes;

import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.security.CorsAccessControlConfiguration;
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.entando.kubernetes.model.common.EntandoMultiTenancy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@ComponentScan("org.entando")
@EnableConfigurationProperties(CorsAccessControlConfiguration.class)
public class EntandoKubernetesJavaApplication {

    static {
        log.info("=== starting app with 'primary' tenant ===");
        TenantContextHolder.setCurrentTenantCode(EntandoMultiTenancy.PRIMARY_TENANT);
    }

    public static void main(final String[] args) {
        SpringApplication.run(EntandoKubernetesJavaApplication.class, args);
    }
    
}
