package org.entando.kubernetes.service.digitalexchange.entandocore;

import org.entando.kubernetes.model.entandocore.EntandoCoreContentModel;
import org.entando.kubernetes.model.entandocore.EntandoCoreFile;
import org.entando.kubernetes.model.entandocore.EntandoCoreFolder;
import org.entando.kubernetes.model.entandocore.EntandoCorePageModel;
import org.entando.kubernetes.model.entandocore.EntandoCoreWidget;
import org.entando.kubernetes.model.bundle.descriptor.ContentModelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageModelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
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

    public EntandoCoreService(@Value("${keycloak.resource}") final String clientId,
                                @Value("${keycloak.credentials.secret}") final String clientSecret,
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
        restTemplate.postForEntity(resolveUrl("/api/widgets").build().toUri(), new EntandoCoreWidget(descriptor), Void.class);
    }

    public void deleteWidget(final String code) {
        restTemplate.delete(resolveUrl(String.format("/api/widgets/%s", code)).build().toUri());
    }

    public void registerLabel(final LabelDescriptor descriptor) {
        restTemplate.postForEntity(resolveUrl("/api/labels").build().toUri(), descriptor, Void.class);
    }

    public void deleteLabel(final String code) {
        restTemplate.delete(resolveUrl(String.format("/api/labels/%s", code)).build().toUri());
    }

    public void registerPageModel(final PageModelDescriptor descriptor) {
        restTemplate.postForEntity(resolveUrl("/api/pageModels").build().toUri(), new EntandoCorePageModel(descriptor), Void.class);
    }

    public void deletePageModel(final String code) {
        restTemplate.delete(resolveUrl(String.format("/api/pageModels/%s", code)).build().toUri());
    }

    public void deleteContentModel(final String code) {
        restTemplate.delete(resolveUrl(String.format("/api/plugins/cms/contentmodels/%s", code)).build().toUri());
    }

    public void registerContentModel(final ContentModelDescriptor descriptor) {
        restTemplate.postForEntity(resolveUrl("/api/plugins/cms/contentmodels").build().toUri(), new EntandoCoreContentModel(descriptor), Void.class);
    }

    public void registerContentType(final ContentTypeDescriptor descriptor) {
        restTemplate.postForEntity(resolveUrl("/api/plugins/cms/contentTypes").build().toUri(), descriptor, Void.class);
    }

    public void deleteContentType(final String code) {
        restTemplate.delete(resolveUrl(String.format("/api/plugins/cms/contentTypes/%s", code)).build().toUri());
    }

    public void createFolder(final String folder) {
        restTemplate.postForEntity(resolveUrl("/api/fileBrowser/directory").build().toUri(), new EntandoCoreFolder(folder), Void.class);
    }

    public void deleteFolder(final String code) {
        final UriComponentsBuilder builder = resolveUrl("/api/fileBrowser/directory");
        builder.queryParam("protectedFolder", "false");
        builder.queryParam("currentPath", code);
        restTemplate.delete(builder.build().toUri());
    }

    public void uploadFile(final FileDescriptor descriptor) {
        final String path = descriptor.getFolder() + "/" + descriptor.getFilename();
        final EntandoCoreFile file = new EntandoCoreFile(false, path, descriptor.getFilename(), descriptor.getBase64());
        restTemplate.postForEntity(resolveUrl("/api/fileBrowser/file").build().toUri(), file, Void.class);
    }

    private UriComponentsBuilder resolveUrl(final String uri) {
        return UriComponentsBuilder.fromUriString(entandoUrl + uri);
    }

}
