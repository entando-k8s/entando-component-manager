package org.entando.kubernetes.config;

import org.entando.kubernetes.model.bundle.BundleDownloader;
import org.entando.kubernetes.model.bundle.NpmBundleDownloader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class AppConfiguration {

    @Bean
    public BundleDownloader bundleDownloader() {
        return new NpmBundleDownloader();
    }

}
