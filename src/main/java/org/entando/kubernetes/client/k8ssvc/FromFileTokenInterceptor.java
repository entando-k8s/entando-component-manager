package org.entando.kubernetes.client.k8ssvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

@Slf4j
public class FromFileTokenInterceptor implements ClientHttpRequestInterceptor {

    private final Path tokenFilePath;
    private volatile String cachedToken;
    private volatile Instant lastReadTime = Instant.MIN;
    private final Duration cacheTtl;

    public FromFileTokenInterceptor(Path tokenFilePath, long cacheTtlSeconds) {
        this.tokenFilePath = tokenFilePath;
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
        getToken();
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        //Inject current cached token
        String token = getToken();
        request.getHeaders().setBearerAuth(token);

        ClientHttpResponse response = execution.execute(request, body);

        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.warn("Received 401 Unauthorized from K8s. Token might be expired. Forcing refresh and retrying...");

            //Force invalidation and reload from disk
            synchronized (this) {
                reloadToken();
            }
            request.getHeaders().setBearerAuth(this.cachedToken);

            // Retry the request
            response.close();
            return execution.execute(request, body);
        }

        return response;
    }

    private String getToken() {
        if (isCacheExpired()) {
            synchronized (this) {
                reloadToken();
            }
        }
        return cachedToken;
    }

    private boolean isCacheExpired() {
        return cachedToken == null || Instant.now().isAfter(lastReadTime.plus(cacheTtl));
    }

    private void reloadToken() {
        if (isCacheExpired()) {
            try {
                log.debug("Reading Kubernetes Service Account token from disk: {}", tokenFilePath);
                this.cachedToken = Files.readString(tokenFilePath).trim();
                this.lastReadTime = Instant.now();
            } catch (IOException e) {
                throw new EntandoComponentManagerException(
                        String.format("Issues retrieving service account token from %s", this.tokenFilePath), e);
            }
        }
    }
}