package org.entando.kubernetes.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppEngineRestTemplateConfiguration {

    public static final String APP_ENGINE_OAUTH2_CLIENT = "app-engine-oauth2-client";

    @Bean(APP_ENGINE_OAUTH2_CLIENT)
    RestTemplate appEngineClient(RestTemplateBuilder restTemplateBuilder, OAuth2AuthorizedClientManager manager,
            ClientRegistrationRepository clientRegistrationRepository) {

        var clientRegistration = clientRegistrationRepository.findByRegistrationId("oidc");

        return restTemplateBuilder
                .additionalInterceptors(new AppEngineRestTemplateInterceptor(manager, clientRegistration))
                .requestFactory(() -> getRequestFactory())
                .build();
    }

    @Bean
    OAuth2AuthorizedClientManager appEngineAuthorizedClientManager(ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .refreshToken()
                        .clientCredentials()
                        .build();
        DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    private ClientHttpRequestFactory getRequestFactory() {
        final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        final int timeout = 10000;

        requestFactory.setConnectionRequestTimeout(timeout);
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return requestFactory;
    }


}
