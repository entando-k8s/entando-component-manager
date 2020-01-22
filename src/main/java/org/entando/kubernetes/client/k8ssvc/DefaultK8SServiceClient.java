package org.entando.kubernetes.client.k8ssvc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.hateoas.mvc.TypeReferences.ResourceType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class DefaultK8SServiceClient implements K8SServiceClient {

    public static final String LINKS = "links";
    public static final String DE_BUNDLES_API_ROOT = "de-bundles";
    private static final Logger LOGGER = Logger.getLogger(DefaultK8SServiceClient.class.getName());
    private final String k8sServiceUrl;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUri;
    private RestTemplate restTemplate;

    public DefaultK8SServiceClient(@Value("${entando.k8s.service.url}") String k8sServiceUrl,
            @Value("${keycloak.resource}") String clientId,
            @Value("${keycloak.credentials.secret}") String clientSecret,
            @Value("${entando.auth-url}") String tokenUri) {
        this.k8sServiceUrl = k8sServiceUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUri = tokenUri;
        this.restTemplate = newRestTemplate();
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<EntandoAppPluginLink> getAppLinkedPlugins(String entandoAppName, String entandoAppNamespace) {
        String url = UriComponentsBuilder.fromUriString(k8sServiceUrl)
                .pathSegment("apps", entandoAppNamespace, entandoAppName, LINKS).toUriString();
        ResponseEntity<Resources<Resource<EntandoAppPluginLink>>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Resources<Resource<EntandoAppPluginLink>>>() {
                });
        if (!responseEntity.hasBody() || responseEntity.getStatusCode().isError()) {
            throw new K8SServiceClientException(
                    String.format("An error occurred (%d-%s) while retriving links for app %s in namespace %s",
                            responseEntity.getStatusCode().value(),
                            responseEntity.getStatusCode().getReasonPhrase(),
                            entandoAppName,
                            entandoAppNamespace)
            );
        }
        return Objects.requireNonNull(responseEntity.getBody()).getContent().stream()
                .map(Resource::getContent)
                .collect(Collectors.toList());

    }

    @Override
    public EntandoPlugin getPluginForLink(EntandoAppPluginLink el) {
        String pluginName = el.getSpec().getEntandoPluginName();
        String pluginNamespace = el.getSpec().getEntandoPluginNamespace();
        String url = UriComponentsBuilder.fromUriString(k8sServiceUrl)
                .pathSegment("plugins", pluginNamespace, pluginName).toUriString();
        ResponseEntity<Resource<EntandoPlugin>> responseEntity = restTemplate
                .exchange(url, HttpMethod.GET, null, new ResourceType<>());
        if (!responseEntity.hasBody() || responseEntity.getStatusCode().isError()) {
            throw new K8SServiceClientException(
                    String.format("An error occurred (%d-%s) while searching plugin %s in namespace %s",
                            responseEntity.getStatusCode().value(),
                            responseEntity.getStatusCode().getReasonPhrase(),
                            pluginName,
                            pluginNamespace
                    ));
        }
        return Objects.requireNonNull(responseEntity.getBody()).getContent();
    }

    @Override
    public void unlink(EntandoAppPluginLink el) {
        String appNamespace = el.getSpec().getEntandoAppNamespace();
        String appName = el.getSpec().getEntandoAppName();
        String pluginName = el.getSpec().getEntandoPluginName();
        String url = UriComponentsBuilder.fromUriString(k8sServiceUrl)
                .pathSegment("apps", appNamespace, appName, LINKS, pluginName).toUriString();
        ResponseEntity<Resources<Resource<EntandoAppPluginLink>>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Resources<Resource<EntandoAppPluginLink>>>() {
                });
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new K8SServiceClientException(
                    String.format("An error occurred (%d-%s) while remove link between app %s in namespace %s and plugin %s",
                            responseEntity.getStatusCode().value(),
                            responseEntity.getStatusCode().getReasonPhrase(),
                            appName,
                            appNamespace,
                            pluginName
                    ));
        }

    }

    @Override
    public void linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin) {
        String url = UriComponentsBuilder.fromUriString(k8sServiceUrl)
                .pathSegment("apps", namespace, name, LINKS).toUriString();
        ResponseEntity<Resources<Resource<EntandoAppPluginLink>>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(plugin),
                new ParameterizedTypeReference<Resources<Resource<EntandoAppPluginLink>>>() {
                });
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new K8SServiceClientException(
                    String.format("An error occurred (%d-%s) while linking app %s in namespace %s to plugin %s",
                            responseEntity.getStatusCode().value(),
                            responseEntity.getStatusCode().getReasonPhrase(),
                            name,
                            namespace,
                            plugin.getMetadata().getName()
                    ));
        }

    }

    @Override
    public List<EntandoDeBundle> getBundlesInDefaultNamespace() {
        String url = UriComponentsBuilder.fromUriString(k8sServiceUrl)
                .pathSegment(DE_BUNDLES_API_ROOT).toUriString();
        return submitBundleRequestAndExtractBody(url);
    }

    @Override
    public List<EntandoDeBundle> getBundlesInNamespace(String namespace) {
        String url = UriComponentsBuilder.fromUriString(k8sServiceUrl)
                .pathSegment(DE_BUNDLES_API_ROOT, "namespaces", namespace)
                .toUriString();
        return submitBundleRequestAndExtractBody(url);
    }

    @Override
    public List<EntandoDeBundle> getBundlesInNamespaces(List<String> namespaces) {
        @SuppressWarnings("unchecked")
        CompletableFuture<List<EntandoDeBundle>>[] futures = namespaces.stream()
                .map(n -> CompletableFuture.supplyAsync(() -> getBundlesInNamespace(n)))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures).thenApply(v -> Arrays.stream(futures)
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
        ).join();
    }

    @Override
    public Optional<EntandoDeBundle> getBundleWithName(String name) {
        return getBundlesInDefaultNamespace().stream()
                .filter(b -> b.getSpec().getDetails().getName().equals(name))
                .findAny();
    }

    @Override
    public Optional<EntandoDeBundle> getBundleWithNameAndNamespace(String name, String namespace) {
        String url = UriComponentsBuilder.fromUriString(k8sServiceUrl)
                .pathSegment(DE_BUNDLES_API_ROOT, "namespaces", namespace, name)
                .toUriString();
        ResponseEntity<Resource<EntandoDeBundle>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Resource<EntandoDeBundle>>() {
                });

        if (responseEntity.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            return Optional.empty();
        }

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new K8SServiceClientException(
                    String.format("An error occurred (%d-%s) while retrieving all available digital-exchange bundles",
                            responseEntity.getStatusCode().value(),
                            responseEntity.getStatusCode().getReasonPhrase()
                    ));
        }

        return Optional.of(Objects.requireNonNull(responseEntity.getBody()).getContent());
    }

    private List<EntandoDeBundle> submitBundleRequestAndExtractBody(String url) {
        ResponseEntity<Resources<Resource<EntandoDeBundle>>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Resources<Resource<EntandoDeBundle>>>() {
                });
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new K8SServiceClientException(
                    String.format("An error occurred (%d-%s) while retrieving all available digital-exchange bundles",
                            responseEntity.getStatusCode().value(),
                            responseEntity.getStatusCode().getReasonPhrase()
                    ));
        }

        return Objects.requireNonNull(responseEntity.getBody())
                .getContent()
                .stream()
                .map(Resource::getContent)
                .collect(Collectors.toList());
    }

    private RestTemplate newRestTemplate() {
        OAuth2ProtectedResourceDetails resourceDetails = getResourceDetails();
        if (resourceDetails == null) {
            return null;
        }
        final OAuth2RestTemplate template = new OAuth2RestTemplate(resourceDetails);
        template.setRequestFactory(getRequestFactory());
        template.setAccessTokenProvider(new ClientCredentialsAccessTokenProvider());

        final List<HttpMessageConverter<?>> converters = template.getMessageConverters();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jackson2HalModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(
                ResourceSupport.class);
        halConverter.setObjectMapper(mapper);
        halConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON, MediaTypes.HAL_JSON));
        converters.add(0, halConverter);

        template.setMessageConverters(converters);

        return template;
    }

    private OAuth2ProtectedResourceDetails getResourceDetails() {
        final ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
        resourceDetails.setAuthenticationScheme(AuthenticationScheme.header);
        resourceDetails.setClientId(clientId);
        resourceDetails.setClientSecret(clientSecret);
        resourceDetails.setClientAuthenticationScheme(AuthenticationScheme.form);
        try {
            resourceDetails.setAccessTokenUri(UriComponentsBuilder.fromUriString(tokenUri).toUriString());
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.SEVERE, ex, () -> String.format("Issues when using %s as token uri", tokenUri));
            return null;
        }
        return resourceDetails;
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
