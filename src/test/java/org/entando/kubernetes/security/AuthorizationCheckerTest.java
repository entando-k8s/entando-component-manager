package org.entando.kubernetes.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.config.tenant.TenantRestTemplateAccessor;
import org.entando.kubernetes.exception.web.AuthorizationDeniedException;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.security.AuthorizationChecker.MyGroupPermission;
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
class AuthorizationCheckerTest {

    @Mock
    private TenantRestTemplateAccessor accessor;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ResponseEntity<SimpleRestResponse<List<MyGroupPermission>>> response;
    @Mock
    private SimpleRestResponse<List<MyGroupPermission>> simpleRestResponse;

    private AuthorizationChecker target;

    @BeforeEach
    public void setup() {
        this.target = new AuthorizationChecker("url", accessor);
        when(accessor.getRestTemplate()).thenReturn(this.restTemplate);
    }

    @Test
    void shouldThrowExceptionWithEmptyOrNullHeader() {
        // null authorization header
        assertThrows(AuthorizationDeniedException.class, () -> target.checkPermissions(null));
        // empty authorization header
        assertThrows(AuthorizationDeniedException.class, () -> target.checkPermissions(""));
    }

    @Test
    void shouldThrowExceptionWhileNotReceivingTheExpectedPermissionFromCore() {

        when(restTemplate.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class))).thenReturn(
                response);
        when(response.getBody()).thenReturn(simpleRestResponse);

        // with empty list of permissions
        when(simpleRestResponse.getPayload()).thenReturn(Collections.emptyList());
        // will fail
        assertThrows(AuthorizationDeniedException.class, () -> target.checkPermissions("jwt"));

        // with a non relevant list of permissions
        final List<MyGroupPermission> myGroupPermissionList = Arrays.asList(
                new MyGroupPermission("mygr-1", Collections.singletonList("my-perm-1")),
                new MyGroupPermission("mygr-2", Arrays.asList("my-perm-2", "my-perm-3")));
        when(simpleRestResponse.getPayload()).thenReturn(myGroupPermissionList);
        // will fail
        assertThrows(AuthorizationDeniedException.class, () -> target.checkPermissions("jwt"));
    }

    @Test
    void shouldNOTThrowExceptionWhileReceivingTheExpectedPermissionFromCore() {

        when(restTemplate.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class))).thenReturn(
                response);
        when(response.getBody()).thenReturn(simpleRestResponse);

        // with superuser permissions
        List<MyGroupPermission> myGroupPermissionList = Arrays.asList(
                new MyGroupPermission("mygr-1", Collections.singletonList("superuser")),
                new MyGroupPermission("mygr-2", Arrays.asList("my-perm-2", "my-perm-3")));
        when(simpleRestResponse.getPayload()).thenReturn(myGroupPermissionList);
        // will succeed
        assertDoesNotThrow(() -> target.checkPermissions("jwt"));

        // with enterECR permissions
        myGroupPermissionList = Arrays.asList(
                new MyGroupPermission("mygr-1", Collections.singletonList("enterECR")),
                new MyGroupPermission("mygr-2", Arrays.asList("my-perm-2", "my-perm-3")));
        when(simpleRestResponse.getPayload()).thenReturn(myGroupPermissionList);
        // will succeed
        assertDoesNotThrow(() -> target.checkPermissions("jwt"));
    }
}
