package org.entando.kubernetes.config;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Setter;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.NpmBundleDownloader;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.processor.PageProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@Setter
public class AppConfiguration {

    @Value("${entando.bundle.type:git}")
    public String type;

//    @Value("${entando.componentManager.processor.page.enabled:false}")
//    private boolean pageProcessorEnabled;

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
        return appContext.getBeansOfType(ComponentProcessor.class).values().stream()
//                .filter(processor -> !(processor instanceof PageProcessor) || pageProcessorEnabled)
                .collect(Collectors.toMap(ComponentProcessor::getSupportedComponentType, Function.identity()));
    }
}
