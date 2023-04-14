package org.entando.kubernetes.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.entando.kubernetes.client.k8ssvc.FromFileTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class K8sServiceRestTemplateConfiguration {

    public static final String K8s_SERVICE_AUTH_CLIENT = "k8s-auth-client";
    public static final String NO_AUTH_REST_CLIENT = "no-auth-rest-template";

    @Value("${entando.k8s.service-account.token-filepath}")
    private String serviceAccountTokenPath;

    @Bean(K8s_SERVICE_AUTH_CLIENT)
    RestTemplate oauth2RestTemplate(RestTemplateBuilder restTemplateBuilder) {

        FromFileTokenProvider provider = FromFileTokenProvider.getInstance(Paths.get(serviceAccountTokenPath));
        return restTemplateBuilder
                .additionalInterceptors(new K8sRestTemplateInterceptor(provider))
                .requestFactory(() -> getRequestFactory())
                .messageConverters(getMessageConverters())
                .build();
    }

    @Bean(NO_AUTH_REST_CLIENT)
    RestTemplate noauthRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .requestFactory(() -> getRequestFactory())
                .messageConverters(getMessageConverters())
                .build();
    }

    private List<HttpMessageConverter<?>> getMessageConverters() {
        List<HttpMessageConverter<?>> messageConverters = Traverson
                .getDefaultMessageConverters(MediaType.APPLICATION_JSON, MediaTypes.HAL_JSON);
        if (messageConverters.stream()
                .noneMatch(mc -> mc.getSupportedMediaTypes().contains(MediaType.APPLICATION_JSON))) {
            messageConverters.add(0, getJsonConverter());
        }

        return messageConverters;
    }

    private HttpMessageConverter<?> getJsonConverter() {
        final List<MediaType> supportedMediatypes = Arrays.asList(MediaType.APPLICATION_JSON);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jackson2HalModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

        converter.setObjectMapper(mapper);
        converter.setSupportedMediaTypes(supportedMediatypes);

        return converter;
    }

    private ClientHttpRequestFactory getRequestFactory() {
        final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        final int timeout = 10000;

        requestFactory.setConnectionRequestTimeout(timeout);
        requestFactory.setConnectTimeout(timeout);
        //requestFactory.setReadTimeout(timeout);
        return requestFactory;
    }


}
