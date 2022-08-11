package org.entando.kubernetes.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthorizationFilter extends HttpFilter {

    public static final Pattern SERVLET_PATH_REGEX = Pattern.compile("^(/components/).*((/installplans)|(/install)|(/uninstall))$");
    protected static final List<String> ECR_PERMISSION_LIST = Arrays.asList("superuser", "enterECR");

    private final RestTemplate restTemplate;
    private final String entandoUrl;

    public AuthorizationFilter(@Value("${entando.url}") final String entandoUrl, RestTemplate simpleRestTemplate) {
        this.entandoUrl = entandoUrl;
        this.restTemplate = simpleRestTemplate;
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final String servletPath = httpRequest.getServletPath();

        if (! SERVLET_PATH_REGEX.matcher(servletPath).matches()) {

            final String perm = fetchAndExtractRequiredPermission(httpRequest);

            if (perm == null) {
                HttpServletResponse resp = ((HttpServletResponse) response);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Permissions included in the JWT don't allow the requested operation");
            } else {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }


    /**
     * fetch and extract the required permission.
     * @param httpRequest the HttpServletRequest from which getting the JWT to send to core
     * @return the extracted permissions or null if not present
     */
    private String fetchAndExtractRequiredPermission(HttpServletRequest httpRequest) {

        final ResponseEntity<SimpleRestResponse<List<MyGroupPermission>>> response = fetchMyGroupPermissions(
                httpRequest);
        return extractRequiredPermission(response);
    }


    /**
     * fetch group permissions from core using the received JWT.
     *
     * @param httpRequest the HttpServletRequest from which getting the JWT to send to core
     * @return the result of the call
     */
    private ResponseEntity<SimpleRestResponse<List<MyGroupPermission>>> fetchMyGroupPermissions(
            HttpServletRequest httpRequest) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", httpRequest.getHeader("Authorization"));

        return restTemplate.exchange(
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

        return response.getBody() == null || response.getBody().getPayload() == null
                ? null
                : response.getBody().getPayload().stream()
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
