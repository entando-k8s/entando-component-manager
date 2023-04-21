package org.entando.kubernetes.client.k8ssvc;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.client.model.EntandoAppPluginLinkJavaNative;
import org.entando.kubernetes.client.model.EntandoDeBundleJavaNative;
import org.entando.kubernetes.client.model.EntandoPluginJavaNative;
import org.entando.kubernetes.client.model.IngressJavaNative;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class DefaultK8SServiceClient implements K8SServiceClient {

    public static final String APPS_ENDPOINT = "apps";
    public static final String PLUGINS_ENDPOINT = "plugins";
    public static final String BUNDLES_ENDPOINT = "bundles";
    public static final String APP_PLUGIN_LINKS_ENDPOINT = "app-plugin-links";
    public static final String ERROR_RETRIEVING_BUNDLE_WITH_NAME = "An error occurred while retrieving bundle with name ";

    public static final String ENTANDO_APP_NAME = "ENTANDO_APP_NAME";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultK8SServiceClient.class);
    private final String k8sServiceUrl;
    private RestTemplate restTemplate;
    private RestTemplate noAuthRestTemplate;
    private final String entandoAppName;

    public DefaultK8SServiceClient(
            RestTemplate oauth2RestTemplate,
            RestTemplate noAuthRestTemplate,
            String k8sServiceUrl, boolean normalizeK8sServiceUrl) {

        if (normalizeK8sServiceUrl && !k8sServiceUrl.endsWith("/")) {
            k8sServiceUrl += "/";
        }
        this.k8sServiceUrl = k8sServiceUrl;
        this.entandoAppName = System.getenv(ENTANDO_APP_NAME);

        this.restTemplate = oauth2RestTemplate;
        this.noAuthRestTemplate = noAuthRestTemplate;

    }

    @Override
    public List<EntandoAppPluginLink> getAppLinks(String entandoAppName) {

        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl).path(APP_PLUGIN_LINKS_ENDPOINT)
                .queryParam("app", entandoAppName).build().toUri();
        LOGGER.info("### fetching EntandoAppPluginLink list from entandoAppName:'{}' with url:'{}'", entandoAppName,
                url);

        return tryOrThrow(() -> {
            return Stream.of(restTemplate.getForObject(url, EntandoAppPluginLinkJavaNative[].class))
                    .map(e -> new EntandoAppPluginLink(e.getMetadata(), e.getSpec(), e.getStatus()))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public EntandoPlugin getPluginForLink(EntandoAppPluginLink el) {
        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl).path(APP_PLUGIN_LINKS_ENDPOINT)
                .queryParam("name", entandoAppName).build().toUri();
        LOGGER.info("### fetching EntandoAppPlugin from EntandoAppPluginLink:'{}' with url:'{}'", el, url);

        return getLinkByName(el.getMetadata().getName())
                .flatMap(link -> getPluginByName(link.getSpec().getEntandoPluginName()))
                .orElse(null);
        /*
        return tryOrThrow(() -> traverson.follow(APP_PLUGIN_LINKS_ENDPOINT)
                .follow(Hop.rel("app-plugin-link").withParameter("name", el.getMetadata().getName()))
                .follow("plugin")
                .toObject(new ParameterizedTypeReference<EntityModel<EntandoPlugin>>() {
                })
                .getContent(), "get plugin associated with link " + el.getMetadata().getName());
         */
    }

    @Override
    public Optional<EntandoPlugin> getPluginByName(String name) {
        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl).path(PLUGINS_ENDPOINT)
                .path("/" + name)
                .build().toUri();
        LOGGER.info("### fetching EntandoPlugin from name:'{}' with url:'{}'", name, url);

        try {
            return Optional.ofNullable(restTemplate.getForObject(url, EntandoPluginJavaNative.class))
                    .map(p -> new EntandoPlugin(p.getMetadata(), p.getSpec(), p.getStatus()));

        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() != 404) {
                throw new KubernetesClientException("An error occurred while retrieving plugin with name " + name, ex);
            }
            return Optional.empty();
        }
    }

    /**
     * UPDATES A PLUGIN.
     * <p>
     * updates a plugin CR given the CR object
     * </p>
     */
    @Override
    public EntandoPlugin updatePlugin(EntandoPlugin plugin) {
        String pluginName = plugin.getMetadata().getName();
        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl).path(PLUGINS_ENDPOINT)
                .build().toUri();
        LOGGER.info("### update  EntandoPlugin with pluginName:'{}' with url:'{}'", pluginName, url);

        /*
        URI updateURI = traverson.follow(PLUGINS_ENDPOINT)
                .follow(Hop.rel("create-or-replace-plugin").withParameter("name", pluginName))
                .asLink().toUri();
         */

        return tryOrThrow(() -> {
            RequestEntity<EntandoPlugin> request = RequestEntity
                    .put(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(plugin);

            ResponseEntity<EntandoPlugin> response = restTemplate
                    .exchange(request, new ParameterizedTypeReference<EntandoPlugin>() {
                    });

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RestClientResponseException("Update process failed",
                        response.getStatusCodeValue(),
                        HttpStatus.valueOf(response.getStatusCode().value()).getReasonPhrase(),
                        null, null, null);
            }
        }, String.format("while updating plugin %s", pluginName));

    }

    @Override
    public void unlink(EntandoAppPluginLink el) {
        String linkName = el.getMetadata().getName();
        String appName = el.getSpec().getEntandoAppName();
        String pluginName = el.getSpec().getEntandoAppNamespace().orElse(el.getMetadata().getNamespace());

        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl)
                .path(APP_PLUGIN_LINKS_ENDPOINT)
                .path("/" + linkName)
                .build().toUri();
        LOGGER.info("### delete EntandoAppPluginLink from linkName:'{}' with url:'{}'", linkName, url);

        tryOrThrow(() -> restTemplate.delete(url),
                String.format("unlink app %s and plugin %s", appName, pluginName));
    }

    @Override
    public void unlinkAndScaleDown(EntandoAppPluginLink el) {
        String linkName = el.getMetadata().getName();
        String appName = el.getSpec().getEntandoAppName();
        String pluginName = el.getSpec().getEntandoAppNamespace().orElse(el.getMetadata().getNamespace());

        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl)
                .path(APP_PLUGIN_LINKS_ENDPOINT)
                .path("/delete-and-scale-down/" + linkName)
                .build().toUri();
        LOGGER.info("### delete and scale down EntandoAppPluginLink from linkName:'{}' with url:'{}'", linkName, url);

        tryOrThrow(() -> restTemplate.delete(url),
                String.format("unlink app %s and plugin %s and scale down plugin", appName, pluginName));
    }

    @Override
    public void removeIngressPathForPlugin(String pluginCode) {
        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl)
                .path(PLUGINS_ENDPOINT)
                .path("/ingress/" + pluginCode)
                .build().toUri();
        LOGGER.info("### delete ingress path for pluginCode:'{}' with url:'{}'", pluginCode, url);

        tryOrThrow(() -> restTemplate.delete(url),
                String.format("remove ingress path from plugin %s", pluginCode));
    }

    @Override
    public EntandoAppPluginLink linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin) {
        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl).path(APPS_ENDPOINT)
                .path("/" + name)
                .path("/links")
                .build().toUri();
        LOGGER.info("### link app name:'{}' with plugin:'{}' with url:'{}'", name, plugin, url);

        /*
        URI linkToCall = tryOrThrow(() -> {
            Link linkToApp = traverson.follow("apps")
                    .follow(Hop.rel("app").withParameter("name", name))
                    .asLink();
            return UriComponentsBuilder.fromUri(linkToApp.toUri()).pathSegment("links").build(Collections.emptyMap());
        });
         */

        return tryOrThrow(() -> {
                    RequestEntity<EntandoPlugin> request = RequestEntity
                            .post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(plugin);
                    ResponseEntity<EntandoAppPluginLink> resp = restTemplate
                            .exchange(request, new ParameterizedTypeReference<EntandoAppPluginLink>() {
                            });
                    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                        return resp.getBody();
                    }
                    throw new RestClientResponseException("Linking process failed",
                            resp.getStatusCodeValue(),
                            HttpStatus.valueOf(resp.getStatusCode().value()).getReasonPhrase(),
                            null, null, null);
                },
                String.format("linking app %s to plugin %s", name, plugin.getMetadata().getName())
        );
    }

    @Override
    public Optional<EntandoAppPluginLink> getLinkByName(String linkName) {
        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl)
                .path(APP_PLUGIN_LINKS_ENDPOINT)
                .path("/" + linkName)
                .build().toUri();
        LOGGER.info("### fetching EntandoAppPluginLink from linkName:'{}' with url:'{}'", linkName, url);

        try {
            return Optional.ofNullable(restTemplate.getForObject(url, EntandoAppPluginLinkJavaNative.class))
                    .map(l -> new EntandoAppPluginLink(l.getMetadata(), l.getSpec(), l.getStatus()));
        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() != 404) {
                throw new KubernetesClientException(
                        "An error occurred while retrieving entando-app-plugin-link with name " + linkName, ex);
            }
            return Optional.empty();
        }
    }

    @Override
    public List<EntandoDeBundle> getBundlesInObservedNamespaces(Optional<String> repoUrlFilter) {

        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl).path(BUNDLES_ENDPOINT)
                .queryParamIfPresent("repoUrl", repoUrlFilter).build().toUri();
        LOGGER.info("### fetching bundles from all namespaces with url:'{}'", url);
        RequestEntity<Void> req = RequestEntity
                .get(url).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.ACCEPT_ENCODING, "none").build();
        List<EntandoDeBundle> bundles = tryOrThrow(
                () -> Stream.of(restTemplate.exchange(req, EntandoDeBundleJavaNative[].class).getBody())
                        .map(b -> new EntandoDeBundle(b.getMetadata(), b.getSpec(), b.getStatus()))
                        .collect(Collectors.toList())
        );

        LOGGER.debug("### from all namespaces fetched bundles:'{}'", bundles);
        return bundles;
    }

    @Override
    public List<EntandoDeBundle> getBundlesInNamespace(String namespace, Optional<String> repoUrlFilter) {

        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl).path(BUNDLES_ENDPOINT)
                .queryParam("namespace", namespace)
                .queryParamIfPresent("repoUrl", repoUrlFilter).build().toUri();

        LOGGER.info("### fetching bundles from namespace:'{}' with url:'{}'", namespace, url);
        RequestEntity<Void> req = RequestEntity
                .get(url).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.ACCEPT_ENCODING, "none").build();
        List<EntandoDeBundle> bundles = tryOrThrow(
                () -> Stream.of(restTemplate.exchange(req, EntandoDeBundleJavaNative[].class).getBody())
                        .map(b -> new EntandoDeBundle(b.getMetadata(), b.getSpec(), b.getStatus()))
                        .collect(Collectors.toList())
        );
        LOGGER.debug("### from namespace:'{}' fetched bundles:'{}'", namespace, bundles);
        return bundles;
        /*
        Hop baseHop = Hop.rel(BUNDLES_ENDPOINT).withParameter("namespace", namespace);
        return tryOrThrow(() -> traverson.follow(
                        repoUrlFilter.map(filter -> baseHop.withParameter("repoUrl", filter)).orElse(baseHop))
                .toObject(new ParameterizedTypeReference<CollectionModel<EntityModel<EntandoDeBundle>>>() {
                })
                .getContent()
                .stream().map(EntityModel::getContent)
                .collect(Collectors.toList()));
                 */
    }

    @Override
    public List<EntandoDeBundle> getBundlesInNamespaces(List<String> namespaces, Optional<String> repoUrlFilter) {
        LOGGER.debug("search bundles in namespaces:'{}' with repoUrlFilter:'{}'", namespaces, repoUrlFilter);
        @SuppressWarnings("unchecked")
        CompletableFuture<List<EntandoDeBundle>>[] futures = namespaces.stream()
                .map(n -> CompletableFuture.supplyAsync(() -> getBundlesInNamespace(n, repoUrlFilter))
                        .exceptionally(ex -> {
                            LOGGER.error("An error occurred while retrieving bundle from a namespace", ex);
                            return Collections.emptyList();
                        }))
                .toArray(CompletableFuture[]::new);

        List<EntandoDeBundle> bundlesInNamespaces = CompletableFuture.allOf(futures)
                .thenApply(v -> Arrays.stream(futures)
                        .map(CompletableFuture::join)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList())
                ).join();
        LOGGER.trace("bundles found in namespaces:'{}'", bundlesInNamespaces);
        return bundlesInNamespaces;
    }

    @Override
    public Optional<EntandoDeBundle> getBundleWithName(String name) {
        return getBundleWithNameAndNamespace(name, Optional.empty());
    }

    @Override
    public Optional<EntandoDeBundle> getBundleWithNameAndNamespace(String name, String namespace) {
        return getBundleWithNameAndNamespace(name, Optional.ofNullable(namespace));
    }

    private Optional<EntandoDeBundle> getBundleWithNameAndNamespace(String name, Optional<String> namespace) {
        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl).path(BUNDLES_ENDPOINT)
                .path("/" + name)
                .queryParamIfPresent("namespace", namespace)
                .build().toUri();

        LOGGER.info("### fetching bundle with name:'{}' with url:'{}'", name, url);
        RequestEntity<Void> req = RequestEntity
                .get(url).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.ACCEPT_ENCODING, "none").build();

        try {
            return Optional.ofNullable(restTemplate.exchange(req, EntandoDeBundleJavaNative.class).getBody())
                    .map(b -> new EntandoDeBundle(b.getMetadata(), b.getSpec(), b.getStatus()));
        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() != 404) {
                throw new KubernetesClientException(ERROR_RETRIEVING_BUNDLE_WITH_NAME + name, ex);
            }
            return Optional.empty();

        } catch (Exception ex) {
            throw new KubernetesClientException(ERROR_RETRIEVING_BUNDLE_WITH_NAME + name, ex);
        }
    }


    @Override
    public boolean isPluginReadyToServeApp(EntandoPlugin plugin, String appName) {
        if (plugin.getSpec().getIngressPath() == null) {
            return false;
        }
        Ingress appIngress = getAppIngress(appName);
        IngressRule ingressRule = appIngress.getSpec().getRules().stream().findFirst().<RuntimeException>orElseThrow(
                () -> {
                    throw new K8SServiceClientException(
                            "EntandoApp ingress " + appIngress.getMetadata().getName() + " does not have an host");
                });

        String appHost = ingressRule.getHost();
        UriComponents pluginHealthCheck = UriComponentsBuilder.newInstance()
                .scheme(appIngress.getSpec().getTls().isEmpty() ? "http" : "https")
                .host(appHost)
                .path(plugin.getSpec().getIngressPath())
                .path(plugin.getSpec().getHealthCheckPath())
                .build();

        String log = "Verifying plugin health check on " + pluginHealthCheck.toUriString();
        LOGGER.info(log);

        RequestEntity<?> request = RequestEntity
                .get(URI.create(pluginHealthCheck.toUriString()))
                .accept(MediaType.APPLICATION_JSON)
                .build();
        try {
            ResponseEntity<Object> response = this.noAuthRestTemplate.exchange(request, Object.class);

            log = String.format("Plugin is%s ready", response.getStatusCode().is2xxSuccessful() ? "" : " NOT");
            LOGGER.info(log);

            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientResponseException e) {
            HttpStatus status = HttpStatus.valueOf(e.getRawStatusCode());
            if (status.equals(HttpStatus.NOT_FOUND) || status.equals(HttpStatus.SERVICE_UNAVAILABLE)) {
                return false;
            }
            throw e;
        } catch (RestClientException e) {
            throw e;
        }

    }

    @Override
    public AnalysisReport getAnalysisReport(List<Reportable> reportableList) {

        Map<String, Status> pluginStatusMap = reportableList.stream()
                .filter(reportable -> reportable.getComponentType() == ComponentType.PLUGIN)
                .flatMap(reportable -> reportable.getComponents().stream())
                .map(comp ->
                        getPluginByName(comp.getCode())
                                .map(plugin -> composeReportableEntry(plugin, comp))
                                .orElseGet(() -> new SimpleEntry<>(comp.getCode(), Status.NEW)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        return new AnalysisReport().setPlugins(pluginStatusMap);
    }

    /**
     * check if the received EntandoPlugin and Reportable.Component correspond to the same plugin version.
     *
     * @param plugin the EntandoPlugin of which check the version
     * @param component the Reportable.Component of which check the version
     * @return the SimpleEntry resulting from the comparison
     */
    private SimpleEntry<String, Status> composeReportableEntry(EntandoPlugin plugin, Reportable.Component component) {
        Status status = Status.EQUAL;
        final DockerImage dockerImage = DockerImage.fromString(plugin.getSpec().getImage());

        if (dockerImage.getSha256() == null
                || !dockerImage.getSha256().equals(component.getVersion())) {

            status = Status.DIFF;
        }
        String pluginCode = plugin.getMetadata().getName();
        boolean isPluginLinked = checkIfPluginIsLinked(pluginCode);
        if (!isPluginLinked) {
            LOGGER.info("plugin with pluginCode:'{}' is linked:'{}' force status DIFF in install plan to reinstall",
                    pluginCode,
                    isPluginLinked);
            status = Status.DIFF;
        }
        return new SimpleEntry<>(component.getCode(), status);
    }

    public boolean checkIfPluginIsLinked(String pluginId) {
        return getAppLinks(this.entandoAppName)
                .stream()
                .anyMatch(el -> el.getSpec().getEntandoPluginName().equals(pluginId));
    }

    @Override
    public EntandoDeBundle deployDeBundle(EntandoDeBundle entandoDeBundle) {

        String logMessage = String.format("### deploy bundle %s",
                ObjectUtils.isEmpty(entandoDeBundle.getMetadata().getName())
                        ? entandoDeBundle.getSpec().getDetails().getName()
                        : entandoDeBundle.getMetadata().getName());

        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl).path(BUNDLES_ENDPOINT)
                .build().toUri();
        LOGGER.debug("### deployDeBundle:'{}' with url:'{}'", entandoDeBundle, url);

        RequestEntity<EntandoDeBundle> request = RequestEntity
                .post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(entandoDeBundle);

        return tryOrThrow(() -> {

            ResponseEntity<Void> response = restTemplate
                    .exchange(request, Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return entandoDeBundle;
            }
            throw new RestClientResponseException("Deploy EntandoDeBundle process failed",
                    response.getStatusCodeValue(),
                    HttpStatus.valueOf(response.getStatusCode().value()).getReasonPhrase(),
                    null, null, null);
        }, logMessage);
    }

    @Override
    public void undeployDeBundle(String bundleName) {

        if (ObjectUtils.isEmpty(bundleName)) {
            throw new KubernetesClientException("Trying to delete an EntandoDeBundle using an empty name");
        }

        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl).path(BUNDLES_ENDPOINT)
                .path("/" + bundleName)
                .build().toUri();
        LOGGER.info("### undeploy EntandoDeBundle from bundleName:'{}' with url:'{}'", bundleName, url);

        try {
            restTemplate.delete(url);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw e;
            }
        }
    }

    @Override
    public ApplicationStatus getAppStatusPhase(String appName) {

        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl).path(APPS_ENDPOINT).path("/" + appName)
                .path("/status")
                .build().toUri();
        LOGGER.info("### fetching app status for appName:'{}' with url:'{}'", appName, url);

        ApplicationStatus status = tryOrThrow(
                () -> restTemplate.getForObject(url, ApplicationStatus.class)
        );

        LOGGER.info("### fetched app status:'{}' for appName:'{}'", status, appName);
        return status;
    }

    private Ingress getAppIngress(String appName) {

        URI url = UriComponentsBuilder.fromUriString(k8sServiceUrl).path(APPS_ENDPOINT).path("/" + appName)
                .path("/ingress")
                .build().toUri();
        LOGGER.info("### fetching app ingress for appName:'{}' with url:'{}'", appName, url);

        return tryOrThrow(
                () -> Optional.ofNullable(restTemplate.getForObject(url, IngressJavaNative.class))
                        .map(i -> {
                            Ingress ing = new Ingress();
                            ing.setMetadata(i.getMetadata());
                            ing.setSpec(i.getSpec());
                            ing.setStatus(i.getStatus());
                            return ing;
                        }).orElse(null)
        );


        /*
        () -> traverson.follow(APPS_ENDPOINT)
        .follow(Hop.rel("app").withParameter("name", appName))
        .follow("app-ingress")
        .toObject(new ParameterizedTypeReference<EntityModel<Ingress>>() {
        })
        .getContent());
         */
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
            throw new KubernetesClientException("A generic error occurred while " + actionDescription, ex);
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
            throw new KubernetesClientException("A generic error occurred while " + action, ex);
        }
    }

}
