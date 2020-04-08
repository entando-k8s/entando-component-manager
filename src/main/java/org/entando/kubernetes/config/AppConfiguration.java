package org.entando.kubernetes.config;

import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class AppConfiguration {

    @Value("${entando.bundle.type:git}")
    public String type;

    @Bean
    public BundleDownloader bundleDownloader() {
        return BundleDownloader.getForType(type);
    }

}
