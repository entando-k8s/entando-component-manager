package org.entando.kubernetes.utils;

import static org.springframework.cloud.contract.wiremock.WireMockSpring.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public abstract class EntandoGenericMockServer {

    protected static int port;
    protected WireMockServer wireMockServer;

    static {
        port = findFreePort().orElse(9080);
    }

    protected EntandoGenericMockServer() {
        wireMockServer = new WireMockServer(options().port(port));
        init(wireMockServer);
        if (!wireMockServer.isRunning()) {
            wireMockServer.start();
        }
    }

    protected abstract void init(WireMockServer wireMockServer);

    public void start() {
        wireMockServer.start();
    }

    public void stop() {
        wireMockServer.stop();
    }

    public void tearDown() {
        wireMockServer.resetAll();
        wireMockServer.stop();
    }

    public void resetRequests() {
        wireMockServer.resetRequests();
    }

    public void resetMappings() {
        wireMockServer.resetAll();
        init(wireMockServer);
    }

    public WireMockServer getInnerServer() {
       return wireMockServer;
    }

    public void addStub(MappingBuilder stub) {
        wireMockServer.stubFor(stub);
    }

    public String getApiRoot() {
        return "http://localhost:" + port;
    }

    public String readResourceAsString(String resourcePath) {

        try
        {
            Path rp = Paths.get(this.getClass().getResource(resourcePath).toURI());
            String content = new String ( Files.readAllBytes(rp) );
            content = content.replaceAll("localhost:9080", "localhost:"+ port);
            return content;
        }
        catch (IOException | URISyntaxException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected static Optional<Integer> findFreePort() {
        Integer port = null;
        try {
            // Get a free port
            ServerSocket s = new ServerSocket(0);
            port = s.getLocalPort();
            s.close();

        } catch (IOException e) {
            // No OPS
        }
        return Optional.ofNullable(port);
    }
}