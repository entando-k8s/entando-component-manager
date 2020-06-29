package org.entando.kubernetes.client.core;

import com.jayway.jsonpath.JsonPath;
import java.nio.file.Paths;
import java.util.List;
import org.entando.kubernetes.exception.web.HttpException;
import org.entando.kubernetes.model.bundle.descriptor.ContentTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreContentModel;
import org.entando.kubernetes.model.entandocore.EntandoCoreFile;
import org.entando.kubernetes.model.entandocore.EntandoCoreFolder;
import org.entando.kubernetes.model.entandocore.EntandoCoreFragment;
import org.entando.kubernetes.model.entandocore.EntandoCorePage;
import org.entando.kubernetes.model.entandocore.EntandoCorePageModel;
import org.entando.kubernetes.model.entandocore.EntandoCoreWidget;
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
        ResponseEntity<SimpleRestResponse<EntandoCoreComponentUsage>> usage = restTemplate
                .exchange(resolvePathSegments("api", "widgets", code, "usage").build().toUri(), HttpMethod.GET, null,
                        new ParameterizedTypeReference<SimpleRestResponse<EntandoCoreComponentUsage>>() {
                        });
        if (usage.getStatusCode().is2xxSuccessful()) {
            return usage.getBody().getPayload();
        } else {
            throw new HttpException(usage.getStatusCode(),
                    "Some error occurred while retrieving widget " + code + " usage");
        }
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
        ResponseEntity<SimpleRestResponse<EntandoCoreComponentUsage>> usage = restTemplate
                .exchange(resolvePathSegments("api", "fragments", code, "usage").build().toUri(), HttpMethod.GET, null,
                        new ParameterizedTypeReference<SimpleRestResponse<EntandoCoreComponentUsage>>() {
                        });
        if (usage.getStatusCode().is2xxSuccessful()) {
            return usage.getBody().getPayload();
        } else {
            throw new HttpException(usage.getStatusCode(),
                    "Some error occurred while retrieving fragment " + code + " usage");
        }
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
        ResponseEntity<SimpleRestResponse<EntandoCoreComponentUsage>> usage = restTemplate
                .exchange(resolvePathSegments("api", "pages", code, "usage").build().toUri(), HttpMethod.GET, null,
                        new ParameterizedTypeReference<SimpleRestResponse<EntandoCoreComponentUsage>>() {
                        });
        if (usage.getStatusCode().is2xxSuccessful()) {
            return usage.getBody().getPayload();
        } else {
            throw new HttpException(usage.getStatusCode(),
                    "Some error occurred while retrieving page " + code + " usage");
        }
    }

    @Override
    public void registerPageModel(final PageTemplateDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "pageModels").build().toUri(),
                new EntandoCorePageModel(descriptor), Void.class);
    }

    @Override
    public void deletePageModel(final String code) {
        restTemplate.delete(resolvePathSegments("api", "pageModels", code).build().toUri());
    }

    @Override
    public EntandoCoreComponentUsage getPageModelUsage(String code) {
        ResponseEntity<SimpleRestResponse<EntandoCoreComponentUsage>> usage = restTemplate
                .exchange(resolvePathSegments("api", "pageModels", code, "usage").build().toUri(), HttpMethod.GET, null,
                        new ParameterizedTypeReference<SimpleRestResponse<EntandoCoreComponentUsage>>() {
                        });
        if (usage.getStatusCode().is2xxSuccessful()) {
            return usage.getBody().getPayload();
        } else {
            throw new HttpException(usage.getStatusCode(),
                    "Some error occurred while retrieving page model " + code + " usage");
        }
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
        ResponseEntity<String> usage = restTemplate
                .exchange(resolvePathSegments("api", "plugins", "cms", "contentmodels", code, "pagereferences").build().toUri(),
                        HttpMethod.GET, null, String.class);
        if (usage.getStatusCode().is2xxSuccessful()) {
            List<String> contentIds = JsonPath.read(usage.getBody(), "$.payload.*.contentsId.*");
            return new EntandoCoreComponentUsage(
                    ComponentType.CONTENT_TEMPLATE.getTypeName(),
                    code,
                    contentIds.size()
            );
        } else {
            throw new HttpException(usage.getStatusCode(),
                    "Some error occurred while retrieving content type " + code + " usage");
        }
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
        ResponseEntity<SimpleRestResponse<EntandoCoreComponentUsage>> usage = restTemplate
                .exchange(resolvePathSegments("api", "plugins", "cms", "contentTypes", code, "usage").build().toUri(),
                        HttpMethod.GET, null,
                        new ParameterizedTypeReference<SimpleRestResponse<EntandoCoreComponentUsage>>() {
                        });
        if (usage.getStatusCode().is2xxSuccessful()) {
            return usage.getBody().getPayload();
        } else {
            throw new HttpException(usage.getStatusCode(),
                    "Some error occurred while retrieving content type " + code + " usage");
        }
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

}
