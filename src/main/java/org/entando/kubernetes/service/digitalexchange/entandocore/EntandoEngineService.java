package org.entando.kubernetes.service.digitalexchange.entandocore;

import org.entando.kubernetes.service.digitalexchange.entandocore.model.EntandoCoreFile;
import org.entando.kubernetes.service.digitalexchange.entandocore.model.EntandoCoreFolder;
import org.entando.kubernetes.service.digitalexchange.entandocore.model.EntandoCorePageModel;
import org.entando.kubernetes.service.digitalexchange.entandocore.model.EntandoCoreWidget;
import org.entando.kubernetes.service.digitalexchange.job.model.FileDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.PageModelDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.WidgetDescriptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Service;

@Service
public class EntandoEngineService {

    private final OAuth2RestTemplate restTemplate;
    private final String entandoUrl;

    public EntandoEngineService(@Value("${keycloak.resource}") final String clientId,
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
        restTemplate.postForEntity(resolveUrl("/api/widgets"), new EntandoCoreWidget(descriptor), Void.class);
    }

    public void registerPageModel(final PageModelDescriptor descriptor) {
        restTemplate.postForEntity(resolveUrl("/api/pageModels"), new EntandoCorePageModel(descriptor), Void.class);
    }

    public void createFolder(final String folder) {
        restTemplate.postForEntity(resolveUrl("/api/fileBrowser/directory"), new EntandoCoreFolder(folder), Void.class);
    }

    public void uploadFile(final FileDescriptor descriptor) {
        final String path = descriptor.getFolder() + "/" + descriptor.getFilename();
        final EntandoCoreFile file = new EntandoCoreFile(false, path, descriptor.getFilename(), descriptor.getBase64());
        restTemplate.postForEntity(resolveUrl("/api/fileBrowser/file"), file, Void.class);
    }

    private String resolveUrl(final String uri) {
        return entandoUrl + uri;
    }

}
