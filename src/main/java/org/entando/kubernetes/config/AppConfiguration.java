package org.entando.kubernetes.config;

import java.util.EnumMap;
import java.util.Map;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader.Type;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.NpmBundleDownloader;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class AppConfiguration {

    @Value("${entando.bundle.type:git}")
    public String type;

    @Bean
    public BundleDownloaderFactory bundleDownloaderFactory() {
        BundleDownloaderFactory factory = new BundleDownloaderFactory();
        if (type.equalsIgnoreCase("npm")) {
            factory.setDefaultSupplier(NpmBundleDownloader::new);
        } else {
            factory.setDefaultSupplier(GitBundleDownloader::new);
        }
        return factory;
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
