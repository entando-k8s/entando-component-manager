package org.entando.kubernetes.client.k8ssvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

@Tag("unit")
class FromFileTokenProviderTest {

    private final String k8sTokenFilePath = "src/test/resources/k8s-service-account-token";
    private final Path k8sTokenPath = Paths.get(k8sTokenFilePath);
    private FromFileTokenProvider fromFileTokenProvider;
    private String tokenValue;

    @BeforeEach
    public void setup() throws IOException {
        tokenValue = Files.readString(k8sTokenPath).trim();
        this.fromFileTokenProvider = FromFileTokenProvider.getInstance(k8sTokenPath);
    }

    @Test
    void shouldObtainTheTokenReadFromTheReceivedPath() {
        OAuth2AccessToken oauth2AccessToken = fromFileTokenProvider.getAccessToken();
        Assertions.assertEquals(tokenValue, oauth2AccessToken.getTokenValue());
    }

    @Test
    void shouldThrowExceptionIfThereceivedPathDoesNotExist() {
        Path notExistingPath = Paths.get("not_existing");
        Assertions.assertThrows(
                EntandoComponentManagerException.class, () -> FromFileTokenProvider.getInstance(notExistingPath));
    }

    /*
    // FIXME
    @Test
    void shouldSupportResources() {
        Assertions.assertTrue(fromFileTokenProvider.supportsResource(null)).isTrue();
    }

    // FIXME
    @Test
    void shouldSupportRefresh() {
        assertThat(fromFileTokenProvider.supportsRefresh(null)).isTrue();
    }

    // FIXME
    @Test
    void shouldRefreshTheTokenIfRequested() throws IOException {

        Path tempFilePath = Paths.get("src/test/resources/TEMP-k8s-service-account-token");
        String token = "customToken";

        try {
            Files.writeString(tempFilePath, token, StandardOpenOption.CREATE_NEW);

            FromFileTokenProvider fromFileTokenProvider = mock(FromFileTokenProvider.class);
            when(fromFileTokenProvider.getTokenFileUri()).thenReturn(tempFilePath);
            when(fromFileTokenProvider.refreshAccessToken(any(), any(), any())).thenCallRealMethod();

            OAuth2AccessToken oauth2AccessToken = fromFileTokenProvider.refreshAccessToken(null, null, null);
            assertThat(oauth2AccessToken.getValue()).isEqualTo(token);
        } finally {
            // clean up
            Files.delete(tempFilePath);
        }
    }
    */
}
