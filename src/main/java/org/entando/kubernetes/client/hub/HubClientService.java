package org.entando.kubernetes.client.hub;

import org.entando.kubernetes.client.hub.domain.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Map;

@Service
public class HubClientService {


    public ProxiedPayload searchBundleGroupVersions(String host, Map<String, Object> params) {
        return doPagedGet(host, BUNDLEGROUPS_API_PATH,
                new ParameterizedTypeReference<PagedContent<BundleGroupVersionFilteredResponseView,
                        BundleGroupVersionEntityDto>>() {}, params);
    }

    public ProxiedPayload getBundles(String host, Map<String, Object> params) {
        return doPagedGet(host, BUNDLE_API_PATH,
                new ParameterizedTypeReference<PagedContent<BundleDto,
                        BundleEntityDto>>() {}, params);
    }

    private static ProxiedPayload doPagedGet(String host, String apiPath, ParameterizedTypeReference typedContent, Map<String, Object> params) {
        RestTemplate restTemplate = new RestTemplate();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(host);
        ProxiedPayload payload;
        try {
            generateUriBuilder(params, builder);
            builder.path(apiPath);
            final String endpointUrl = builder.build().toString();
            ResponseEntity response = restTemplate.exchange(endpointUrl, HttpMethod.GET, null, typedContent);
            payload = ProxiedPayload.builder()
                    .payload(response.getBody())
                    .status(response.getStatusCode())
                    .build();
        } catch (Throwable t) {
            t.printStackTrace(); // FIXME usare log
            payload = ProxiedPayload.builder()
                    .exceptionMessage(t.getMessage())
                    .exceptionClass(t.getClass().getCanonicalName())
                    .build();
        }
        return payload;
    }

    @Deprecated
    protected ProxiedPayload doGet(String host, String apiPath, Map<String, Object> params) {
        ProxiedPayload payload;
        RestTemplate restTemplate = new RestTemplate();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(host);

        try {
            generateUriBuilder(params, builder);
            builder.path(apiPath);
            final String endpointUrl = builder.build().toString();
            ResponseEntity<String> response
                    = restTemplate.getForEntity(endpointUrl, String.class);
            payload = ProxiedPayload.builder()
                    .payload(response.getBody())
                    .status(response.getStatusCode())
                    .build();
        } catch (Throwable t) {
            payload = ProxiedPayload.builder()
                    .exceptionMessage(t.getMessage())
                    .exceptionClass(t.getClass().getCanonicalName())
                    .build();
        }
        return payload;
    }

    /**
     * Creates endpoint URL
     * @param params map query parameters (value can be a String[])
     * @param builder the generated builder
     */
    private static void generateUriBuilder(Map<String, Object> params, UriComponentsBuilder builder) {
        if (params != null)
            params.entrySet().stream()
                    .forEach(e -> {
                        if (e.getValue() instanceof String[]) {
                            Arrays.stream((String [])e.getValue())
                                    .forEach(l -> builder.queryParam(e.getKey(), l));
                        } else {
                            builder.queryParam(e.getKey(), e.getValue());
                        }
                    });
    }

    public static final String BUNDLEGROUPS_API_PATH = "appbuilder/api/bundlegroups/";
    public static final String BUNDLE_API_PATH = "appbuilder/api/bundles/";
}
