package org.entando.kubernetes.client.k8ssvc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class DefaultK8SServiceClient implements K8SServiceClient {

    private static final Logger LOGGER = Logger.getLogger(DefaultK8SServiceClient.class.getName());
    public static final String LINKS = "links";
    public static final String DE_BUNDLES_API_ROOT = "de-bundles";

    private final String k8sServiceUrl;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUri;
    private RestTemplate restTemplate;
    private final Traverson traverson;

    public DefaultK8SServiceClient(String k8sServiceUrl, String clientId, String clientSecret, String tokenUri)  {
        this.k8sServiceUrl = k8sServiceUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUri = tokenUri;
        this.restTemplate = newRestTemplate();
        this.traverson = new Traverson(URI.create(this.k8sServiceUrl), MediaTypes.HAL_JSON)
                .setRestOperations(newRestTemplate());
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<EntandoAppPluginLink> getAppLinkedPlugins(String entandoAppName, String entandoAppNamespace) {
        return tryOrThrow(() -> {
            CollectionModel<EntityModel<EntandoAppPluginLink>> links = traverson
                    .follow("apps")
                    .follow(Hop.rel("app-links").withParameter("name", entandoAppName))
                    .toObject(new ParameterizedTypeReference<CollectionModel<EntityModel<EntandoAppPluginLink>>>(){});
            assert links != null;
            return links.getContent().stream()
                    .map(EntityModel::getContent)
                    .collect(Collectors.toList());

        });
    }

    @Override
    public EntandoPlugin getPluginForLink(EntandoAppPluginLink el) {
        return tryOrThrow(() -> traverson.follow("app-plugin-links")
                .follow(Hop.rel("link").withParameter("name", el.getMetadata().getName()))
                .follow("plugin")
                .toObject(new ParameterizedTypeReference<EntityModel<EntandoPlugin>>() {})
                .getContent());
    }

    @Override
    public void unlink(EntandoAppPluginLink el) {
        String linkName = el.getMetadata().getName();
        String unlinkHref = traverson.follow("app-plugin-links")
                .follow(Hop.rel("link").withParameter("name", linkName))
                .follow(Hop.rel("unlink"))
                .asLink().getHref();
        ResponseEntity<CollectionModel<EntityModel<EntandoAppPluginLink>>> responseEntity = restTemplate.exchange(
                unlinkHref,
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<CollectionModel<EntityModel<EntandoAppPluginLink>>>() {});
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new K8SServiceClientException(
                    String.format("An error occurred (%d-%s) while remove link between app %s in namespace %s and plugin %s",
                            responseEntity.getStatusCode().value(),
                            responseEntity.getStatusCode().getReasonPhrase(),
                            el.getSpec().getEntandoAppName(),
                            el.getSpec().getEntandoAppNamespace(),
                            el.getSpec().getEntandoPluginName()
                    ));
        }

    }

    @Override
    public void linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin) {
        String uriToCall = traverson.follow("apps")
                .follow(Hop.rel("app-links").withParameter("name", name))
                .asLink().getHref();
        ResponseEntity<CollectionModel<EntityModel<EntandoAppPluginLink>>> responseEntity = restTemplate.exchange(
                uriToCall,
                HttpMethod.POST,
                new HttpEntity<>(plugin),
                new ParameterizedTypeReference<CollectionModel<EntityModel<EntandoAppPluginLink>>>() {});
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
    public List<EntandoDeBundle> getBundlesInObservedNamespaces() {
        return tryOrThrow(() -> traverson.follow("bundles")
                .toObject(new ParameterizedTypeReference<CollectionModel<EntityModel<EntandoDeBundle>>>() {})
                .getContent()
                .stream().map(EntityModel::getContent)
                .collect(Collectors.toList()));
    }

    @Override
    public List<EntandoDeBundle> getBundlesInNamespace(String namespace) {
        Map<String, Object> params = new HashMap<>();
        params.put("namespace", namespace);
        return tryOrThrow(() -> traverson.follow("/bundles?{namespace}").withTemplateParameters(params)
                .toObject(new ParameterizedTypeReference<CollectionModel<EntityModel<EntandoDeBundle>>>() {})
                .getContent()
                .stream().map(EntityModel::getContent)
                .collect(Collectors.toList()));
    }

    @Override
    public List<EntandoDeBundle> getBundlesInNamespaces(List<String> namespaces) {
        @SuppressWarnings("unchecked")
        CompletableFuture<List<EntandoDeBundle>>[] futures = namespaces.stream()
                .map(n -> CompletableFuture.supplyAsync(() -> getBundlesInNamespace(n))
                        .exceptionally(ex -> {
                            LOGGER.log(Level.SEVERE, "An error occurred while retrieving bundle from a namespace", ex);
                            return Collections.emptyList();
                        }))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures).thenApply(v -> Arrays.stream(futures)
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
        ).join();
    }

    @Override
    public Optional<EntandoDeBundle> getBundleWithName(String name) {
        return getBundlesInObservedNamespaces().stream()
                .filter(b -> b.getSpec().getDetails().getName().equals(name))
                .findAny();
    }

    @Override
    public Optional<EntandoDeBundle> getBundleWithNameAndNamespace(String name, String namespace) {
        return getBundlesInNamespace(namespace).stream()
                .filter(b -> b.getSpec().getDetails().getName().equals(name))
                .findFirst();
    }


    private RestTemplate newRestTemplate() {
        OAuth2ProtectedResourceDetails resourceDetails = getResourceDetails();
        if (resourceDetails == null) {
            return null;
        }
        final OAuth2RestTemplate template = new OAuth2RestTemplate(resourceDetails);
        template.setRequestFactory(getRequestFactory());
        template.setAccessTokenProvider(new ClientCredentialsAccessTokenProvider());

        List<HttpMessageConverter<?>> converters = template.getMessageConverters();
        converters.add(0, getHalConverter());

        template.setMessageConverters(converters);

        return template;
    }


    private HttpMessageConverter<?> getHalConverter() {
        List<MediaType> supportedMediatypes = Arrays.asList(MediaType.APPLICATION_JSON, MediaTypes.HAL_JSON);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jackson2HalModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

        converter.setObjectMapper(mapper);
        converter.setSupportedMediaTypes(supportedMediatypes);

        return converter;
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

    public <T> T tryOrThrow(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RestClientResponseException ex) {
            throw new KubernetesClientException(
                    String.format("An error occurred while talking with k8s-service: %d - %s",
                            ex.getRawStatusCode(),
                            ex.getResponseBodyAsString()),
                    ex);
        } catch (RestClientException ex) {
            throw new KubernetesClientException( "A generic error occurred while talking with k8s-service", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Something unexpected happened ", ex);
        }
    }

}
