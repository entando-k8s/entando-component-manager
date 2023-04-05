package org.entando.kubernetes.client.hub;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import liquibase.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DefaultHubClient implements HubClient {

    public static final String HUB_API_KEY_HEADER = "Entando-hub-api-key";
    public static final String BUNDLEGROUPS_API_PATH = "/bundlegroups/";
    public static final String BUNDLE_API_PATH = "/bundles/";

    private final Logger log = LoggerFactory.getLogger(DefaultHubClient.class);

    /**
     * Creates endpoint URL.
     *
     * @param params  map query parameters (value can be a String[])
     * @param builder the generated builder
     */
    private static void generateUriBuilder(Map<String, Object> params, UriComponentsBuilder builder) {
        if (params != null) {
            params.forEach((key, value) -> {
                if (value instanceof String[]) {
                    Arrays.stream((String[]) value)
                            .forEach(l -> builder.queryParam(key, l));
                } else {
                    builder.queryParam(key, value);
                }
            });
        }
    }

    @Override
    public ProxiedPayload<PagedContent<BundleGroupVersionFilteredResponseView, BundleGroupVersionEntityDto>> searchBundleGroupVersions(
            EntandoHubRegistry registry, Map<String, Object> params) {
        return doPagedGet(registry, BUNDLEGROUPS_API_PATH,
                new ParameterizedTypeReference<PagedContent<BundleGroupVersionFilteredResponseView,
                        BundleGroupVersionEntityDto>>() {
                }, params);
    }

    @Override
    public ProxiedPayload<PagedContent<BundleDto, BundleEntityDto>> getBundles(EntandoHubRegistry registry,
            Map<String, Object> params) {
        return doPagedGet(registry, BUNDLE_API_PATH,
                new ParameterizedTypeReference<PagedContent<BundleDto, BundleEntityDto>>() {
                }, params);
    }

    protected <T> ProxiedPayload<T> doPagedGet(EntandoHubRegistry registry, String apiPath, ParameterizedTypeReference<? extends PagedContent> typedContent,
            Map<String, Object> params) {
        RestTemplate restTemplate = new RestTemplate();
        ProxiedPayload<T> payload;

        if (registry == null || StringUtils.isEmpty(registry.getUrl())) {
            throw new EntandoComponentManagerException("The received hub registry URL is null or empty");
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(registry.getUrl());
            generateUriBuilder(params, builder);
            builder.path(apiPath);

            HttpEntity<Void> entity = composeWithApiKeyHeader(registry);

            final String endpointUrl = builder.build().toString();
            ResponseEntity<? extends PagedContent> response = restTemplate.exchange(endpointUrl, HttpMethod.GET, entity, typedContent);
            payload = ProxiedPayload.<T>builder()
                    .payload((T) response.getBody())
                    .status(response.getStatusCode())
                    .build();
        } catch (RuntimeException t) {
            log.error("error performing paged GET", t);
            payload = ProxiedPayload.<T>builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .exceptionMessage(t.getMessage())
                    .exceptionClass(t.getClass().getCanonicalName())
                    .build();
        }
        return payload;
    }

    protected HttpEntity<Void> composeWithApiKeyHeader(EntandoHubRegistry registry) {
        HttpEntity<Void> entity = null;
        if (registry.hasApiKey()) {
            HttpHeaders headers = new HttpHeaders();
            headers.put(HUB_API_KEY_HEADER, Collections.singletonList(registry.getApiKey()));
            entity = new HttpEntity<>(headers);
        }
        return entity;
    }
}
