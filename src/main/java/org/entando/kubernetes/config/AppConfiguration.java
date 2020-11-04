package org.entando.kubernetes.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Setter;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.NpmBundleDownloader;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.reportable.AnalysisReportFunction;
import org.entando.kubernetes.model.bundle.reportable.ReportableComponentProcessor;
import org.entando.kubernetes.model.bundle.reportable.ReportableRemoteHandler;
import org.entando.kubernetes.service.KubernetesService;
import org.springframework.beans.factory.annotation.Autowired;
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

    private EntandoCoreClient entandoCoreClient;
    private K8SServiceClient kubernetesServiceClient;

    @Autowired
    public AppConfiguration(EntandoCoreClient entandoCoreClient, K8SServiceClient kubernetesServiceClient) {
        this.entandoCoreClient = entandoCoreClient;
        this.kubernetesServiceClient = kubernetesServiceClient;
    }

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
                .collect(Collectors.toMap(ComponentProcessor::getSupportedComponentType, Function.identity()));
    }

    @Bean
    public List<ReportableComponentProcessor> reportableComponentProcessorList(ApplicationContext appContext) {
        return new ArrayList<>(appContext.getBeansOfType(ReportableComponentProcessor.class).values());
    }

    @Bean
    public Map<ReportableRemoteHandler, AnalysisReportFunction> analysisReportStrategies() {

        return Map.of(
                // ENGINE ANALYSIS
                ReportableRemoteHandler.ENTANDO_ENGINE,
                entandoCoreClient::getEngineAnalysisReport,
                // CMS ANALYSIS
                ReportableRemoteHandler.ENTANDO_CMS,
                entandoCoreClient::getCMSAnalysisReport,
                // K8S SERVICE ANALYSIS
                ReportableRemoteHandler.ENTANDO_K8S_SERVICE,
                kubernetesServiceClient::getAnalysisReport
        );
    }
}
