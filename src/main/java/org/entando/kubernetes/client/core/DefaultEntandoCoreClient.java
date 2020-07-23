package org.entando.kubernetes.client.core;

import org.entando.kubernetes.exception.web.HttpException;
import org.entando.kubernetes.model.bundle.descriptor.*;
import org.entando.kubernetes.model.entandocore.*;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoDefaultOAuth2RequestAuthenticator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Paths;

@Service
public class DefaultEntandoCoreClient implements EntandoCoreClient {

    private final OAuth2RestTemplate restTemplate;
    private final String entandoUrl;

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
        restTemplate.delete(resolvePathSegments("api", "widgets", code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getWidgetUsage(String code) {
        return this.getComponentUsage(code, new String[]{"api", "widgets", code, "usage"}, "widget");
    }

    @Override
    public void registerFragment(FragmentDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "fragments").build().toUri(),
                new EntandoCoreFragment(descriptor), Void.class);
    }

    @Override
    public void deleteFragment(String code) {
        restTemplate.delete(resolvePathSegments("api", "fragments", code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getFragmentUsage(String code) {
        return this.getComponentUsage(code, new String[]{"api", "fragments", code, "usage"}, "fragment");
    }

    @Override
    public void registerLabel(final LabelDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "labels").build().toUri(), descriptor, Void.class);
    }

    @Override
    public void deleteLabel(final String code) {
        restTemplate.delete(resolvePathSegments("api", "labels", code).build().toUri());
    }

    @Override
    public void registerPage(PageDescriptor pageDescriptor) {
        restTemplate
                .postForEntity(resolvePathSegments("api", "pages").build().toUri(), new EntandoCorePage(pageDescriptor),
                        Void.class);
    }

    @Override
    public void deletePage(final String code) {
        restTemplate.delete(resolvePathSegments("api", "pages", code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getPageUsage(String code) {
        return this.getComponentUsage(code, new String[]{"api", "pages", code, "usage"}, "page");
    }

    @Override
    public void registerPageModel(final PageTemplateDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "pageModels").build().toUri(),
                new EntandoCorePageTemplate(descriptor), Void.class);
    }

    @Override
    public void deletePageModel(final String code) {
        restTemplate.delete(resolvePathSegments("api", "pageModels", code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getPageModelUsage(String code) {
        return this.getComponentUsage(code, new String[]{"api", "pageModels", code, "usage"}, "page template");
    }

    @Override
    public void deleteContentModel(final String code) {
        restTemplate.delete(resolvePathSegments("api", "plugins", "cms", "contentmodels", code).build().toUri());
    }

    @Override
    public void registerContentModel(final ContentTemplateDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "plugins", "cms", "contentmodels").build().toUri(),
                new EntandoCoreContentModel(descriptor), Void.class);
    }

    @Override
    public EntandoCoreComponentUsage getContentModelUsage(String code) {
        return this.getComponentUsage(code, new String[]{"api", "plugins", "cms", "contentmodels", code, "usage"}, "content template");
    }

    @Override
    public void registerContentType(final ContentTypeDescriptor descriptor) {
        restTemplate
                .postForEntity(resolvePathSegments("api", "plugins", "cms", "contentTypes").build().toUri(), descriptor,
                        Void.class);
    }

    @Override
    public void deleteContentType(final String code) {
        restTemplate.delete(resolvePathSegments("api", "plugins", "cms", "contentTypes", code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getContentTypeUsage(String code) {
        return this.getComponentUsage(code, new String[]{"api", "plugins", "cms", "contentTypes", code, "usage"}, "content type");
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
        restTemplate.delete(builder.build().toUri());
    }

    @Override
    public void uploadFile(final FileDescriptor descriptor) {
        final String path = Paths.get(descriptor.getFolder(), descriptor.getFilename()).toString();
        final EntandoCoreFile file = new EntandoCoreFile(false, path, descriptor.getFilename(), descriptor.getBase64());
        restTemplate.postForEntity(resolvePathSegments("api", "fileBrowser", "file").build().toUri(), file, Void.class);
    }

    private UriComponentsBuilder resolvePathSegments(String... segments) {
        return UriComponentsBuilder
                .fromUriString(entandoUrl)
                .pathSegment(segments);
    }


    /**
     * asks for component the received component id usage count
     * @param code the code of the component of which get the usage count
     * @param endpointUrlParts a String array containing the query segments representing the endpoint url to qurey
     * @return an instance of EntandoCoreComponentUsage containing the number of the entities using the component
     */
    public EntandoCoreComponentUsage getComponentUsage(String code, String[] endpointUrlParts, String componentType) {

        ResponseEntity<SimpleRestResponse<EntandoCoreComponentUsage>> usage = restTemplate
                .exchange(resolvePathSegments(endpointUrlParts).build().toUri(), HttpMethod.GET, null,
                        new ParameterizedTypeReference<SimpleRestResponse<EntandoCoreComponentUsage>>() {});

        if (usage.getStatusCode().is2xxSuccessful()) {
            return usage.getBody().getPayload();
        } else {
            throw new HttpException(usage.getStatusCode(),
                    String.format("Some error occurred while retrieving %s %s usage", componentType, code));
        }
    }
}
