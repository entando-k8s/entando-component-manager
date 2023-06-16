package org.entando.kubernetes.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.tenant.TenantRestTemplateAccessor;
import org.entando.kubernetes.exception.web.AuthorizationDeniedException;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Slf4j
@Component
public class AuthorizationChecker {

    protected static final String ACCESS_DENIED_ERROR = "The user does not have the required permission to execute this operation";
    protected static final List<String> ECR_PERMISSION_LIST = Arrays.asList("superuser", "enterECR");
    
    private final TenantRestTemplateAccessor restTemplateAccessor;
    private final String entandoUrl;

    public AuthorizationChecker(@Value("${entando.url}") final String entandoUrl, TenantRestTemplateAccessor restTemplateAccessor) {
        this.entandoUrl = entandoUrl;
        this.restTemplateAccessor = restTemplateAccessor;
    }

    public void checkPermissions(String authorizationHeader) {
        if (ObjectUtils.isEmpty(authorizationHeader)) {
            throw new AuthorizationDeniedException(ACCESS_DENIED_ERROR);
        }
        final String perm = fetchAndExtractRequiredPermission(authorizationHeader);
        if (perm == null) {
            throw new AuthorizationDeniedException(ACCESS_DENIED_ERROR);
        }
    }


    /**
     * fetch and extract the required permission.
     *
     * @param authorizationHeader the authorization header to send to core
     * @return the extracted permissions or null if not present
     */
    private String fetchAndExtractRequiredPermission(String authorizationHeader) {

        final ResponseEntity<SimpleRestResponse<List<MyGroupPermission>>> response = fetchMyGroupPermissions(
                authorizationHeader);
        return extractRequiredPermission(response);
    }


    /**
     * fetch group permissions from core using the received JWT.
     *
     * @param authorizationHeader the authorization header to send to core
     * @return the result of the call
     */
    private ResponseEntity<SimpleRestResponse<List<MyGroupPermission>>> fetchMyGroupPermissions(
            String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        return this.restTemplateAccessor.getRestTemplate().exchange(
                entandoUrl + "/api/users/myGroupPermissions", HttpMethod.GET,
                new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<SimpleRestResponse<List<MyGroupPermission>>>() {
                });
    }

    /**
     * try to extract one of the accepted permissions from the received response.
     *
     * @param response the ResponseEntity from which extract the required permission
     * @return the extracted permissions or null if not present
     */
    private String extractRequiredPermission(ResponseEntity<SimpleRestResponse<List<MyGroupPermission>>> response) {

        final var body = response.getBody();
        return body == null || body.getPayload() == null
                ? null
                : body.getPayload().stream()
                        .map(MyGroupPermission::getPermissions)
                        .flatMap(Collection::stream)
                        .filter(ECR_PERMISSION_LIST::contains)
                        .findFirst()
                        .orElse(null);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    protected static class MyGroupPermission {

        private String group;
        private List<String> permissions;
    }
}
