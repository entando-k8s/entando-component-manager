package org.entando.kubernetes.client.hub;

import java.util.Arrays;
import java.util.Map;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionEntityDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DefaultHubClient implements HubClient {

    public static final String BUNDLEGROUPS_API_PATH = "appbuilder/api/bundlegroups/";
    public static final String BUNDLE_API_PATH = "appbuilder/api/bundles/";
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
            String host, Map<String, Object> params) {
        return doPagedGet(host, BUNDLEGROUPS_API_PATH,
                new ParameterizedTypeReference<PagedContent<BundleGroupVersionFilteredResponseView,
                        BundleGroupVersionEntityDto>>() {
                }, params);
    }

    @Override
    public ProxiedPayload<PagedContent<BundleDto, BundleEntityDto>> getBundles(String host,
            Map<String, Object> params) {
        return doPagedGet(host, BUNDLE_API_PATH,
                new ParameterizedTypeReference<PagedContent<BundleDto, BundleEntityDto>>() {
                }, params);
    }

    protected <T> ProxiedPayload<T> doPagedGet(String host, String apiPath, ParameterizedTypeReference<? extends PagedContent> typedContent,
            Map<String, Object> params) {
        RestTemplate restTemplate = new RestTemplate();
        ProxiedPayload<T> payload;

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(host);
            generateUriBuilder(params, builder);
            builder.path(apiPath);
            final String endpointUrl = builder.build().toString();
            ResponseEntity<? extends PagedContent> response = restTemplate.exchange(endpointUrl, HttpMethod.GET, null, typedContent);
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

}
