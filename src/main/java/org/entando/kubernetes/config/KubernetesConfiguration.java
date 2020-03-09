package org.entando.kubernetes.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.entando.kubernetes.client.k8ssvc.DefaultK8SServiceClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class KubernetesConfiguration {

    @Value("${entando.k8s.service.url}")
    private String k8sServiceUrl;
    @Value("${spring.security.oauth2.client.registration.oidc.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.oidc.client-secret}")
    private String clientSecret;
    @Value("${entando.auth-url}")
    private String tokenUri;

    @Bean
    public KubernetesClient client() {
        final Config config = new ConfigBuilder().build();
        return new DefaultKubernetesClient(config);
    }

    @Bean
    public K8SServiceClient k8SServiceClient() {
        return new DefaultK8SServiceClient(k8sServiceUrl, clientId, clientSecret, tokenUri);
    }

}
