package org.entando.kubernetes.config;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

public class AppEngineRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private final OAuth2AuthorizedClientManager manager;
    private final ClientRegistration clientRegistration;
    // private final OAuth2AuthorizeRequest oauth2AuthorizeRequest;

    public AppEngineRestTemplateInterceptor(OAuth2AuthorizedClientManager manager,
            ClientRegistration clientRegistration/*,
            OAuth2AuthorizeRequest oauth2AuthorizeRequest*/) {
        this.manager = manager;
        this.clientRegistration = clientRegistration;
        // this.oauth2AuthorizeRequest = oauth2AuthorizeRequest;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        OAuth2AuthorizeRequest oauth2AuthorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(clientRegistration.getRegistrationId())
                .principal(createPrincipal())
                .build();

        OAuth2AuthorizedClient client = manager.authorize(oauth2AuthorizeRequest);
        if (isNull(client)) {
            throw new IllegalStateException("client credentials flow on " + clientRegistration.getRegistrationId() + " failed, client is null");
        }

        request.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + client.getAccessToken().getTokenValue());
        return execution.execute(request, body);
    }

    private Authentication createPrincipal() {
        return new Authentication() {

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return Collections.emptySet();
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return this;
            }

            @Override
            public boolean isAuthenticated() {
                return false;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }

            @Override
            public String getName() {
                return clientRegistration.getClientId();
            }
        };
    }


}
