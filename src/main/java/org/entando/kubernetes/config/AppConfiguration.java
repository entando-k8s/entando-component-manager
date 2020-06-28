package org.entando.kubernetes.config;

import java.util.HashMap;
import java.util.Map;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class AppConfiguration {

    @Autowired
    ApplicationContext appContext;

    @Value("${entando.bundle.type:git}")
    public String type;

    @Bean
    public BundleDownloader bundleDownloader() {
        return BundleDownloader.getForType(type);
    }

    @Bean
    public Map<ComponentType, ComponentProcessor> processorMap() {
        Map<ComponentType, ComponentProcessor> processorMap = new HashMap<>();
        for(ComponentProcessor pr:appContext.getBeansOfType(ComponentProcessor.class).values()) {
            processorMap.put(pr.getSupportedComponentType(), pr);
        }
        return processorMap;
    }
}
