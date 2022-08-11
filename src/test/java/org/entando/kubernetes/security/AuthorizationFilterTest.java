package org.entando.kubernetes.security;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.security.AuthorizationFilter.MyGroupPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AuthorizationFilterTest {

    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private HttpServletResponse httpResponse;
    @Mock
    private FilterChain filterChain;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ResponseEntity<SimpleRestResponse<List<MyGroupPermission>>> response;
    @Mock
    private SimpleRestResponse<List<MyGroupPermission>> simpleRestResponse;

    private final String entandoUrl = "http://entando.url";
    private AuthorizationFilter target;

    @BeforeEach
    public void setup() {
        target = new AuthorizationFilter(entandoUrl, restTemplate);

        when(httpRequest.getServletPath()).thenReturn("/components/my-bundle-a1b2c3d4/protected");
    }

    private void stubForProtectedEndpoints() {
        when(httpRequest.getHeader("Authorization")).thenReturn(JWT);
        when(restTemplate.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class))).thenReturn(
                response);
        when(response.getBody()).thenReturn(simpleRestResponse);
    }

    @Test
    void givenANotAuthorizedTokenCallingAProtectedEndpointShouldFail() throws Exception {

        stubForProtectedEndpoints();

        // with empty list of permissions
        when(simpleRestResponse.getPayload()).thenReturn(Collections.emptyList());
        // will fail
        target.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpResponse, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN,
                "Permissions included in the JWT don't allow the requested operation");

        // with a non relevant list of permissions
        final List<MyGroupPermission> myGroupPermissionList = Arrays.asList(
                new MyGroupPermission("mygr-1", Collections.singletonList("my-perm-1")),
                new MyGroupPermission("mygr-2", Arrays.asList("my-perm-2", "my-perm-3")));
        when(simpleRestResponse.getPayload()).thenReturn(myGroupPermissionList);
        // will fail
        target.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpResponse, times(2)).sendError(HttpServletResponse.SC_FORBIDDEN,
                "Permissions included in the JWT don't allow the requested operation");
    }

    @Test
    void givenANotAuthorizedTokenCallingANonProtectedEndpointShouldSucceed() throws Exception {

        // given a non protected endpoint
        Stream.of("/install", "/installplans", "/uninstall")
                .forEach(endpoint -> {

                    when(httpRequest.getServletPath()).thenReturn("/components/my-bundle-a1b2c3d4" + endpoint);

                    // will succeed
                    try {
                        target.doFilter(httpRequest, httpResponse, filterChain);
                        verify(httpResponse, times(0)).sendError(HttpServletResponse.SC_FORBIDDEN,
                                "Permissions included in the JWT don't allow the requested operation");
                    } catch (Exception e) {
                        fail(e);
                    }
                });
    }

    @Test
    void givenACorrectlyAuthorizedTokenCallingAProtectedShouldSucceed() throws Exception {

        stubForProtectedEndpoints();

        // with superuser permission
        List<MyGroupPermission> myGroupPermissionList = List.of(
                new MyGroupPermission("mygr-1", Collections.singletonList("superuser")));
        when(simpleRestResponse.getPayload()).thenReturn(myGroupPermissionList);
        // will succeed
        target.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpResponse, times(0)).sendError(HttpServletResponse.SC_FORBIDDEN,
                "Permissions included in the JWT don't allow the requested operation");

        // with enterECR permission
        myGroupPermissionList = List.of(
                new MyGroupPermission("mygr-1", Collections.singletonList("enterECR")));
        when(simpleRestResponse.getPayload()).thenReturn(myGroupPermissionList);
        // will succeed
        target.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpResponse, times(0)).sendError(HttpServletResponse.SC_FORBIDDEN,
                "Permissions included in the JWT don't allow the requested operation");

        // with a valid list of permission
        myGroupPermissionList = List.of(
                new MyGroupPermission("mygr-1", Arrays.asList("superuser", "enterECR")));
        when(simpleRestResponse.getPayload()).thenReturn(myGroupPermissionList);
        // will succeed
        target.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpResponse, times(0)).sendError(HttpServletResponse.SC_FORBIDDEN,
                "Permissions included in the JWT don't allow the requested operation");
    }

    private static final String JWT = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI2UjZxZVBEZC1WUnJNNjdOZWgtWWRkZ"
            + "3p1NExfTU1CMGJITWN1eXhrTmJVIn0.eyJleHAiOjE2NjAyMjk3MzYsImlhdCI6MTY2MDIyOTQzNiwianRpIjoiNGZhYjliZGMtNmU4M"
            + "y00Mzc0LThiOTMtMjUxMzRhMGQ2MGExIiwiaXNzIjoiaHR0cDovL3Rlc3QtYWxlc3Npby5hcHBzLm1haW5saW5lLmVuZy1lbnRhbmRvL"
            + "mNvbS9hdXRoL3JlYWxtcy9lbnRhbmRvIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImEyOWJjODRkLTUxNGItNGE3Mi1hNWI4LTE3ODE1N"
            + "GU4NmUxYyIsInR5cCI6IkJlYXJlciIsImF6cCI6InBvc3RtYW4iLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbIioiXSwicmVhb"
            + "G1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iLCJkZWZhdWx0LXJvbGVzLWVudGFuZ"
            + "G8iXX0sInJlc291cmNlX2FjY2VzcyI6eyJwb3N0bWFuIjp7InJvbGVzIjpbInVtYV9wcm90ZWN0aW9uIl19LCJhY2NvdW50Ijp7InJvb"
            + "GVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2Zpb"
            + "GUgZW1haWwiLCJjbGllbnRIb3N0IjoiOTMuMzUuMTkxLjEzOCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiY2xpZW50SWQiOiJwb3N0b"
            + "WFuIiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2VydmljZS1hY2NvdW50LXBvc3RtYW4iLCJjbGllbnRBZGRyZXNzIjoiOTMuMzUuMTkxL"
            + "jEzOCJ9.Pa_QlLIdnsK31cerCI4CG7-VpQ-_b5OQuqJrGL0d1XYoDzgW0L0bxkOtOrkFrOYG84EIsdzQ_hJyiv426vWgdalwqGsgOMIG"
            + "1N53JUIVVGd-cVtcwt_CmD7wt2JyrV_Vsb_84fcm12J4VPIp7dpkWi1V4Eu8qrWMMPw3jAbQJKHnbKf70yh9MUpqTb37ruxt9NRey66R"
            + "bC7luwjEHUWXfnsGg94Q86oRPztUBNy4zDkx7MKYwDbieDyLt8IiuCo13Tg7r67ClqKFvXHoKRoXqpHahxceo-mVMgqoXRW4tL_JJ41Y"
            + "slrGuZnRgP2RgLuXWaLCg76INwLrUPbtVw93Hw";
}
