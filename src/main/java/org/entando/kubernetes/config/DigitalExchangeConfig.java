package org.entando.kubernetes.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.controller.digitalexchange.model.DigitalExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DigitalExchangeConfig {

    @Value("${entando.digital-exchanges")
    private List<String> digitalExchangesNames;

    @Bean
    public List<DigitalExchange> accessibleDigitalExchanges() {
        List<String> deNames = new ArrayList<>(digitalExchangesNames);
        if (deNames.isEmpty())
            deNames.add(K8SServiceClient.DEFAULT_BUNDLE_NAMESPACE);
        return deNames.stream().map(n -> DigitalExchange.builder().id(n).name(n).active(true).build()).collect(Collectors.toList());
    }

}
