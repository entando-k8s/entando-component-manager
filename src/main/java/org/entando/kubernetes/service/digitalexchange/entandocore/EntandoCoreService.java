package org.entando.kubernetes.service.digitalexchange.entandocore;

import java.nio.file.Paths;
import org.entando.kubernetes.model.bundle.descriptor.ContentModelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageModelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.entandocore.EntandoCoreContentModel;
import org.entando.kubernetes.model.entandocore.EntandoCoreFile;
import org.entando.kubernetes.model.entandocore.EntandoCoreFolder;
import org.entando.kubernetes.model.entandocore.EntandoCoreFragment;
import org.entando.kubernetes.model.entandocore.EntandoCorePage;
import org.entando.kubernetes.model.entandocore.EntandoCorePageModel;
import org.entando.kubernetes.model.entandocore.EntandoCoreWidget;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EntandoCoreService {

    private final OAuth2RestTemplate restTemplate;
    private final String entandoUrl;

    public EntandoCoreService(
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

    public void registerWidget(final WidgetDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "widgets").build().toUri(), new EntandoCoreWidget(descriptor), Void.class);
    }

    public void deleteWidget(final String code) {
        restTemplate.delete(resolvePathSegments("api", "widgets", code).build().toUri());
    }

    public void registerFragment(FragmentDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "fragments").build().toUri(), new EntandoCoreFragment(descriptor), Void.class);
    }

    public void deleteFragment(String code) {
        restTemplate.delete(resolvePathSegments("api","fragments", code).build().toUri());
    }

    public void registerLabel(final LabelDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api","labels").build().toUri(), descriptor, Void.class);
    }

    public void deleteLabel(final String code) {
        restTemplate.delete(resolvePathSegments("api", "labels", code).build().toUri());
    }

    public void registerPage(PageDescriptor pageDescriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "pages").build().toUri(), new EntandoCorePage(pageDescriptor), Void.class);
    }

    public void deletePage(final String code) {
        restTemplate.delete(resolvePathSegments("api", "pages", code).build().toUri());
    }

    public void registerPageModel(final PageModelDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "pageModels").build().toUri(), new EntandoCorePageModel(descriptor), Void.class);
    }

    public void deletePageModel(final String code) {
        restTemplate.delete(resolvePathSegments("api", "pageModels", code).build().toUri());
    }

    public void deleteContentModel(final String code) {
        restTemplate.delete(resolvePathSegments("api", "plugins", "cms", "contentmodels",code).build().toUri());
    }

    public void registerContentModel(final ContentModelDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "plugins", "cms", "contentmodels").build().toUri(), new EntandoCoreContentModel(descriptor), Void.class);
    }

    public void registerContentType(final ContentTypeDescriptor descriptor) {
        restTemplate.postForEntity(resolvePathSegments("api", "plugins", "cms", "contentTypes").build().toUri(), descriptor, Void.class);
    }

    public void deleteContentType(final String code) {
        restTemplate.delete(resolvePathSegments("api", "plugins", "cms", "contentTypes", code).build().toUri());
    }

    public void createFolder(final String folder) {
        restTemplate.postForEntity(resolvePathSegments("api", "fileBrowser", "directory").build().toUri(), new EntandoCoreFolder(folder), Void.class);
    }

    public void deleteFolder(final String code) {
        UriComponentsBuilder builder = resolvePathSegments("api", "fileBrowser", "directory");
        builder.queryParam("protectedFolder", "false");
        builder.queryParam("currentPath", code);
        restTemplate.delete(builder.build().toUri());
    }

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
