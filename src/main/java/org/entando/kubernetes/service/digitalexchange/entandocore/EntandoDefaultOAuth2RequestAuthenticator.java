package org.entando.kubernetes.service.digitalexchange.entandocore;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RequestAuthenticator;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class EntandoDefaultOAuth2RequestAuthenticator implements OAuth2RequestAuthenticator {

    public void authenticate(final OAuth2ProtectedResourceDetails resource,
                             final OAuth2ClientContext clientContext,
                             final ClientHttpRequest request) {
        final OAuth2AccessToken accessToken = clientContext.getAccessToken();
        if (accessToken == null) {
            throw new AccessTokenRequiredException(resource);
        } else {
            request.getHeaders().set("Authorization", String.format("Bearer %s", accessToken.getValue()));
        }
    }

}
