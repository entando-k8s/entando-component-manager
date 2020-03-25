package org.entando.kubernetes.client.k8ssvc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class DefaultK8SServiceClient implements K8SServiceClient {

    private static final Logger LOGGER = Logger.getLogger(DefaultK8SServiceClient.class.getName());
    public static final String BUNDLES_ENDPOINT = "bundles";
    public static final String APP_PLUGIN_LINKS_ENDPOINT = "app-plugin-links";

    private final String k8sServiceUrl;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUri;
    private RestTemplate restTemplate;
    private Traverson traverson;

    public DefaultK8SServiceClient(String k8sServiceUrl, String clientId, String clientSecret, String tokenUri)  {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUri = tokenUri;
        this.k8sServiceUrl = k8sServiceUrl;
        this.restTemplate = newRestTemplate();
        this.traverson = newTraverson();
    }

    public Traverson newTraverson() {
        return new Traverson(URI.create(k8sServiceUrl), MediaTypes.HAL_JSON, MediaType.APPLICATION_JSON)
                .setRestOperations(getRestTemplate());
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.traverson = newTraverson();
    }

    @Override
    public List<EntandoAppPluginLink> getAppLinks(String entandoAppName) {
        return tryOrThrow(() -> {
            CollectionModel<EntityModel<EntandoAppPluginLink>> links = traverson
                    .follow(APP_PLUGIN_LINKS_ENDPOINT)
                    .follow(Hop.rel("app-links").withParameter("app", entandoAppName))
                    .toObject(new ParameterizedTypeReference<CollectionModel<EntityModel<EntandoAppPluginLink>>>(){});
            assert links != null;
            return links.getContent().stream()
                    .map(EntityModel::getContent)
                    .collect(Collectors.toList());

        });
    }

    @Override
    public EntandoPlugin getPluginForLink(EntandoAppPluginLink el) {
        return tryOrThrow(() -> traverson.follow(APP_PLUGIN_LINKS_ENDPOINT)
                .follow(Hop.rel("app-plugin-link").withParameter("name", el.getMetadata().getName()))
                .follow("plugin")
                .toObject(new ParameterizedTypeReference<EntityModel<EntandoPlugin>>() {})
                .getContent(), "get plugin associated with link " + el.getMetadata().getName());
    }

    @Override
    public void unlink(EntandoAppPluginLink el) {
        String linkName = el.getMetadata().getName();
        String appName = el.getSpec().getEntandoAppName();
        String pluginName = el.getSpec().getEntandoAppNamespace();
        Link unlinkHref = traverson.follow(APP_PLUGIN_LINKS_ENDPOINT)
                .follow(Hop.rel("app-plugin-link").withParameter("name", linkName))
                .asLink();
        tryOrThrow(() -> restTemplate.delete(unlinkHref.toUri()), String.format("unlink app %s and plugin %s", appName, pluginName));
    }

    @Override
    public void linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin) {
        Link linkToApp = traverson.follow("apps")
                .follow(Hop.rel("app").withParameter("name", name))
                .asLink();
        URI linkToCall = UriComponentsBuilder.fromUri(linkToApp.toUri()).pathSegment("links").build(Collections.emptyMap());
        tryOrThrow(() -> restTemplate.postForEntity(linkToCall, plugin, Void.class),
                String.format("linking app %s to plugin %s", name, plugin.getMetadata().getName())
        );
    }

    @Override
    public List<EntandoDeBundle> getBundlesInObservedNamespaces() {
        return tryOrThrow(() -> traverson.follow(BUNDLES_ENDPOINT)
                .toObject(new ParameterizedTypeReference<CollectionModel<EntityModel<EntandoDeBundle>>>() {})
                .getContent()
                .stream().map(EntityModel::getContent)
                .collect(Collectors.toList()));
    }

    @Override
    public List<EntandoDeBundle> getBundlesInNamespace(String namespace) {
        return tryOrThrow(() -> traverson.follow(BUNDLES_ENDPOINT)
                .follow(Hop.rel("bundles-in-namespace").withParameter("namespace", namespace))
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
        EntandoDeBundle bundle = null;
        try {
            bundle = traverson.follow(BUNDLES_ENDPOINT)
                    .follow(Hop.rel("bundle").withParameter("name", name))
                    .toObject(new ParameterizedTypeReference<EntityModel<EntandoDeBundle>>() {})
                    .getContent();
            
        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() != 404) {
                throw new KubernetesClientException("An error occurred while retrieving bundle with name " + name, ex);
            }
        }
        return Optional.ofNullable(bundle);
    }

    @Override
    public Optional<EntandoDeBundle> getBundleWithNameAndNamespace(String name, String namespace) {
        return getBundlesInNamespace(namespace).stream()
                .filter(b -> b.getMetadata().getName().equals(name))
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

        template.setMessageConverters(Traverson.getDefaultMessageConverters(MediaTypes.HAL_JSON));

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

    public void tryOrThrow(Runnable runnable, String actionDescription) {
        try {
            runnable.run();
        } catch (RestClientResponseException ex) {
            throw new KubernetesClientException(
                    String.format("An error occurred while %s: %d - %s",
                            actionDescription,
                            ex.getRawStatusCode(),
                            ex.getResponseBodyAsString()),
                    ex);
        } catch (Exception ex) {
            throw new KubernetesClientException( "A generic error occurred while " + actionDescription, ex);
        }
    }

    public <T> T tryOrThrow(Supplier<T> supplier) {
        return tryOrThrow(supplier, "talking with k8s-service");
    }


    public <T> T tryOrThrow(Supplier<T> supplier, String action) {
        try {
            return supplier.get();
        } catch (RestClientResponseException ex) {
            throw new KubernetesClientException(
                    String.format("An error occurred while %s: %d - %s",
                            action,
                            ex.getRawStatusCode(),
                            ex.getResponseBodyAsString()),
                    ex);
        } catch (Exception ex) {
            throw new KubernetesClientException( "A generic error occurred while " + action, ex);
        }
    }

}
