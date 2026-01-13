package org.entando.kubernetes.client.k8ssvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

@Tag("unit")
class FromFileTokenInterceptorTest {

    private final Path k8sTokenPath = Paths.get("src/test/resources/k8s-service-account-token");
    private final long cacheTtlSeconds = 60;

    private FromFileTokenInterceptor interceptor;
    private HttpRequest mockRequest;
    private HttpHeaders httpHeaders;
    private ClientHttpRequestExecution mockExecution;
    private ClientHttpResponse mockResponse;
    private String tokenValue;

    @BeforeEach
    void setup() throws IOException {
        tokenValue = Files.readString(k8sTokenPath).trim();
        interceptor = new FromFileTokenInterceptor(k8sTokenPath, cacheTtlSeconds);
        mockRequest = mock(HttpRequest.class);
        httpHeaders = new HttpHeaders();
        mockExecution = mock(ClientHttpRequestExecution.class);
        mockResponse = mock(ClientHttpResponse.class);

        when(mockRequest.getHeaders()).thenReturn(httpHeaders);
    }

    @Test
    void shouldAddBearerTokenToRequest() throws IOException {
        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);

        ClientHttpResponse response = interceptor.intercept(mockRequest, new byte[0], mockExecution);

        assertThat(response).isEqualTo(mockResponse);
        assertThat(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + tokenValue);
        verify(mockExecution, times(1)).execute(eq(mockRequest), any());
    }

    @Test
    void shouldRetryRequestOn401WithRefreshedToken(@TempDir Path tempDir) throws IOException {
        Path tokenFile = tempDir.resolve("token");
        String initialToken = "initial-token";
        Files.writeString(tokenFile, initialToken, StandardOpenOption.CREATE);

        FromFileTokenInterceptor tempInterceptor = new FromFileTokenInterceptor(tokenFile, 1);
        HttpHeaders headers = new HttpHeaders();
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);

        ClientHttpResponse unauthorizedResponse = mock(ClientHttpResponse.class);
        when(unauthorizedResponse.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);

        ClientHttpResponse successResponse = mock(ClientHttpResponse.class);
        when(successResponse.getStatusCode()).thenReturn(HttpStatus.OK);

        String refreshedToken = "refreshed-token";
        // First call returns 401, second call (after refresh) returns 200
        when(mockExecution.execute(eq(request), any()))
                .thenAnswer(invocation -> {
                    // After first call, update the token file to simulate refresh
                    if (headers.getFirst(HttpHeaders.AUTHORIZATION).equals("Bearer " + initialToken)) {
                        Files.writeString(tokenFile, refreshedToken, StandardOpenOption.TRUNCATE_EXISTING);
                        Thread.sleep(1000);
                        return unauthorizedResponse;
                    }
                    return successResponse;
                });

        ClientHttpResponse response = tempInterceptor.intercept(request, new byte[0], mockExecution);

        assertThat(response).isEqualTo(successResponse);
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + refreshedToken);
        verify(mockExecution, times(2)).execute(eq(request), any());
        verify(unauthorizedResponse).close();
    }

    @Test
    void shouldCacheTokenWithinTtl() throws IOException {
        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);

        // First call
        interceptor.intercept(mockRequest, new byte[0], mockExecution);
        String firstAuthHeader = httpHeaders.getFirst(HttpHeaders.AUTHORIZATION);

        // Second call should use cached token
        interceptor.intercept(mockRequest, new byte[0], mockExecution);
        String secondAuthHeader = httpHeaders.getFirst(HttpHeaders.AUTHORIZATION);

        assertThat(firstAuthHeader).isEqualTo(secondAuthHeader);
        assertThat(firstAuthHeader).isEqualTo("Bearer " + tokenValue);
    }

    @Test
    void shouldThrowExceptionWhenTokenFileDoesNotExist() {
        Path nonExistentPath = Paths.get("non-existent-token-file");

        assertThatThrownBy(() -> new FromFileTokenInterceptor(nonExistentPath, cacheTtlSeconds))
                .isInstanceOf(EntandoComponentManagerException.class)
                .hasMessageContaining("Issues retrieving service account token");
    }

    @Test
    void shouldReturnResponseDirectlyWhenNotUnauthorized() throws IOException {
        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.FORBIDDEN);

        ClientHttpResponse response = interceptor.intercept(mockRequest, new byte[0], mockExecution);

        assertThat(response).isEqualTo(mockResponse);
        verify(mockExecution, times(1)).execute(eq(mockRequest), any());
    }

    @Test
    void shouldHandleTokenFileWithWhitespace(@TempDir Path tempDir) throws IOException {
        Path tokenFile = tempDir.resolve("token-with-whitespace");
        String tokenWithWhitespace = "  my-token-value  \n";
        Files.writeString(tokenFile, tokenWithWhitespace, StandardOpenOption.CREATE);

        HttpHeaders headers = new HttpHeaders();
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);

        when(mockExecution.execute(eq(request), any())).thenReturn(mockResponse);
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);

        FromFileTokenInterceptor tempInterceptor = new FromFileTokenInterceptor(tokenFile, cacheTtlSeconds);
        tempInterceptor.intercept(request, new byte[0], mockExecution);

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer my-token-value");
    }
}