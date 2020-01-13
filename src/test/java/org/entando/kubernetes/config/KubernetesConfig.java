package org.entando.kubernetes.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class KubernetesConfig {

    @Bean
    public KubernetesClient client() {
        return Mockito.mock(KubernetesClient.class);
    }

    @Bean
    @Primary
    public K8SServiceClient k8SServiceClient() {
        return new K8SServiceClientTestDouble();
    }
}
