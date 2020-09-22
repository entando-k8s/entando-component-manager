package org.entando.kubernetes.client.core;

import java.net.URI;
import java.nio.file.Paths;
import org.entando.kubernetes.exception.web.HttpException;
import org.entando.kubernetes.model.bundle.descriptor.CategoryDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ContentTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.GroupDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreContentModel;
import org.entando.kubernetes.model.entandocore.EntandoCoreFile;
import org.entando.kubernetes.model.entandocore.EntandoCoreFolder;
import org.entando.kubernetes.model.entandocore.EntandoCoreFragment;
import org.entando.kubernetes.model.entandocore.EntandoCorePage;
import org.entando.kubernetes.model.entandocore.EntandoCorePageTemplate;
import org.entando.kubernetes.model.entandocore.EntandoCoreWidget;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoDefaultOAuth2RequestAuthenticator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DefaultEntandoCoreClient implements EntandoCoreClient {

    private final OAuth2RestTemplate restTemplate;
    private final String entandoUrl;
    private static final String USAGE_PATH_SEGMENT = "usage";

    public DefaultEntandoCoreClient(
            @Value("${spring.security.oauth2.client.registration.oidc.client-id}") final String clientId,
            @Value("${spring.security.oauth2.client.registration.oidc.client-secret}") final String clientSecret,
            @Value("${entando.auth-url}") final String tokenUri,
            @Value("${entando.url}") final String entandoUrl) {
        final ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
        resourceDetails.setAuthenticationScheme(AuthenticationScheme.header);
        resourceDetails.setClientId(clientId);
        resourceDetails.setClientSecret(clientSecret);
        resourceDetails.setAccessTokenUri(tokenUri);

        this.entandoUrl = entandoUrl;
        this.restTemplate = new OAuth2RestTemplate(resourceDetails);
        this.restTemplate.setAuthenticator(new EntandoDefaultOAuth2RequestAuthenticator());
        this.restTemplate.setAccessTokenProvider(new ClientCredentialsAccessTokenProvider());
    }

    @Override
    public void registerWidget(final WidgetDescriptor descriptor) {
        restTemplate
                .postForEntity(resolvePathSegments("api", "widgets").build().toUri(), new EntandoCoreWidget(descriptor),
                        Void.class);
    }

    @Override
    public void deleteWidget(final String code) {
        notFoundProtectedDelete(resolvePathSegments("api", "widgets", code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getWidgetUsage(String code) {
        return this.getComponentUsage(code, new String[]{"api", "widgets", code, USAGE_PATH_SEGMENT}, "widget");
    }

    @Override
    public void registerFragment(FragmentDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "fragments").build().toUri(),
                new EntandoCoreFragment(descriptor), Void.class);
    }

    @Override
    public void deleteFragment(String code) {
        notFoundProtectedDelete(resolvePathSegments("api", "fragments", code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getFragmentUsage(String code) {
        return this.getComponentUsage(code, new String[]{"api", "fragments", code, USAGE_PATH_SEGMENT}, "fragment");
    }

    @Override
    public void registerLabel(final LabelDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "labels").build().toUri(), descriptor, Void.class);
    }

    @Override
    public void deleteLabel(final String code) {
        notFoundProtectedDelete(resolvePathSegments("api", "labels", code).build().toUri());
    }

    @Override
    public void registerGroup(GroupDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "groups").build().toUri(), descriptor, Void.class);

    }

    @Override
    public void deleteGroup(String code) {
        notFoundProtectedDelete(resolvePathSegments("api", "groups", code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getGroupUsage(String code) {
        return this.getComponentUsage(code, new String[]{"api", "groups", code, USAGE_PATH_SEGMENT}, "group");
    }

    @Override
    public void registerPage(PageDescriptor pageDescriptor) {
        restTemplate
                .postForEntity(resolvePathSegments("api", "pages").build().toUri(), new EntandoCorePage(pageDescriptor),
                        Void.class);
    }

    @Override
    public void deletePage(final String code) {
        notFoundProtectedDelete(resolvePathSegments("api", "pages", code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getPageUsage(String code) {
        return this.getComponentUsage(code, new String[]{"api", "pages", code, USAGE_PATH_SEGMENT}, "page");
    }

    @Override
    public void registerPageModel(final PageTemplateDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "pageModels").build().toUri(),
                new EntandoCorePageTemplate(descriptor), Void.class);
    }

    @Override
    public void deletePageModel(final String code) {
        notFoundProtectedDelete(resolvePathSegments("api", "pageModels", code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getPageModelUsage(String code) {
        return this
                .getComponentUsage(code, new String[]{"api", "pageModels", code, USAGE_PATH_SEGMENT}, "page template");
    }

    @Override
    public void deleteContentModel(final String code) {
        notFoundProtectedDelete(resolvePathSegments("api", "plugins", "cms", "contentmodels", code).build().toUri());
    }

    @Override
    public void registerContentModel(final ContentTemplateDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "plugins", "cms", "contentmodels").build().toUri(),
                new EntandoCoreContentModel(descriptor), Void.class);
    }

    @Override
    public EntandoCoreComponentUsage getContentModelUsage(String code) {
        return this.getComponentUsage(code,
                new String[]{"api", "plugins", "cms", "contentmodels", code, USAGE_PATH_SEGMENT},
                "content template");
    }

    @Override
    public void registerContentType(final ContentTypeDescriptor descriptor) {
        restTemplate
                .postForEntity(resolvePathSegments("api", "plugins", "cms", "contentTypes").build().toUri(), descriptor,
                        Void.class);
    }

    @Override
    public void deleteContentType(final String code) {
        notFoundProtectedDelete(resolvePathSegments("api", "plugins", "cms", "contentTypes", code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getContentTypeUsage(String code) {
        return this
                .getComponentUsage(code,
                        new String[]{"api", "plugins", "cms", "contentTypes", code, USAGE_PATH_SEGMENT},
                        "content type");
    }

    @Override
    public void createFolder(final String folder) {
        restTemplate.postForEntity(resolvePathSegments("api", "fileBrowser", "directory").build().toUri(),
                new EntandoCoreFolder(folder), Void.class);
    }

    @Override
    public void deleteFolder(final String code) {
        UriComponentsBuilder builder = resolvePathSegments("api", "fileBrowser", "directory");
        builder.queryParam("protectedFolder", "false");
        builder.queryParam("currentPath", code);
        notFoundProtectedDelete(builder.build().toUri());
    }

    @Override
    public void uploadFile(final FileDescriptor descriptor) {
        final String path = Paths.get(descriptor.getFolder(), descriptor.getFilename()).toString();
        final EntandoCoreFile file = new EntandoCoreFile(false, path, descriptor.getFilename(), descriptor.getBase64());
        restTemplate.postForEntity(resolvePathSegments("api", "fileBrowser", "file").build().toUri(), file, Void.class);
    }

    @Override
    public void registerCategory(CategoryDescriptor representation) {
        restTemplate
                .postForEntity(resolvePathSegments("api", "categories").build().toUri(), representation, Void.class);
    }

    @Override
    public void deleteCategory(String code) {
        notFoundProtectedDelete(resolvePathSegments("api", "categories", code).build().toUri());

    }

    @Override
    public EntandoCoreComponentUsage getCategoryUsage(String code) {
        return this.getComponentUsage(code, new String[]{"api", "categories", code, USAGE_PATH_SEGMENT}, "category");
    }

    private UriComponentsBuilder resolvePathSegments(String... segments) {
        return UriComponentsBuilder
                .fromUriString(entandoUrl)
                .pathSegment(segments);
    }

    /**
     * asks for component the received component id usage count.
     *
     * @param code             the code of the component of which get the usage count
     * @param endpointUrlParts a String array containing the query segments representing the endpoint url to qurey
     * @return an instance of EntandoCoreComponentUsage containing the number of the entities using the component
     */
    public EntandoCoreComponentUsage getComponentUsage(String code, String[] endpointUrlParts, String componentType) {

        ResponseEntity<SimpleRestResponse<EntandoCoreComponentUsage>> usage = restTemplate
                .exchange(resolvePathSegments(endpointUrlParts).build().toUri(), HttpMethod.GET, null,
                        new ParameterizedTypeReference<SimpleRestResponse<EntandoCoreComponentUsage>>() {
                        });

        if (usage.getStatusCode().is2xxSuccessful()) {
            return usage.getBody().getPayload();
        } else {
            throw new HttpException(usage.getStatusCode(),
                    String.format("Some error occurred while retrieving %s %s usage", componentType, code));
        }
    }

    private void notFoundProtectedDelete(URI url) {
        try {
            restTemplate.delete(url);
        } catch (RestClientResponseException e) {
            if (isSafeDeleteResponseStatus(e.getRawStatusCode())) {
                return;
            }
            throw e;
        }
    }

    private boolean isSafeDeleteResponseStatus(int status) {
        HttpStatus s = HttpStatus.resolve(status);
        return s != null && (s.is2xxSuccessful() || s.equals(HttpStatus.NOT_FOUND));
    }
}
