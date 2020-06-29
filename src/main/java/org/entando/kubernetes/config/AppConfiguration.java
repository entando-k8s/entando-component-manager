package org.entando.kubernetes.config;

import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.EnumMap;
import java.util.Map;

@Configuration
@Profile("!test")
public class AppConfiguration {

    @Value("${entando.bundle.type:git}")
    public String type;

    @Bean
    public BundleDownloader bundleDownloader() {
        return BundleDownloader.getForType(type);
    }

    @Bean
    public Map<ComponentType, ComponentProcessor> processorMap(ApplicationContext appContext) {
        Map<ComponentType, ComponentProcessor> processorMap = new EnumMap<>(ComponentType.class);
        for(ComponentProcessor pr:appContext.getBeansOfType(ComponentProcessor.class).values()) {
            processorMap.put(pr.getSupportedComponentType(), pr);
        }
        return processorMap;
    }
}
