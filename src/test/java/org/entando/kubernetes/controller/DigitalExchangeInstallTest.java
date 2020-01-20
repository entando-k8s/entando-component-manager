package org.entando.kubernetes.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.DigitalExchangeTestUtils.checkRequest;
import static org.entando.kubernetes.DigitalExchangeTestUtils.getTestPrivateKey;
import static org.entando.kubernetes.DigitalExchangeTestUtils.getTestPublicKey;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFile;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFileAsBase64;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.digitalexchange.DigitalExchange;
import org.entando.kubernetes.service.digitalexchange.signature.SignatureUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 8099)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Ignore("Need to be rewritten after building new version")
public class DigitalExchangeInstallTest {

    private static final String URL = "/components";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DatabaseCleaner databaseCleaner;
    @Autowired
    private K8SServiceClient k8SServiceClient;

    @After
    public void cleanup() throws SQLException {
        databaseCleaner.cleanup();
        ((K8SServiceClientTestDouble) k8SServiceClient).cleanInMemoryDatabases();
    }

    @Test
//    @Ignore("This requires to nuke the mocker and use the new k8sServiceClient")
    public void testInstallComponent() throws Exception {

        K8SServiceClientTestDouble k8SServiceClientTestDouble = (K8SServiceClientTestDouble) k8SServiceClient;

        final URI dePKGPath = DigitalExchangeInstallTest.class.getResource("/bundle.zip").toURI();
        final InputStream in = Files.newInputStream(Paths.get(dePKGPath), StandardOpenOption.READ);
        k8SServiceClientTestDouble.addInMemoryBundle(getTestBundle());

        stubFor(WireMock.get("/repository/npm-internal/inail_bundle/-/inail_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage())));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));

        stubFor(WireMock.post(urlEqualTo("/entando-app/api/widgets")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/fileBrowser/file")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/fileBrowser/directory"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/pageModels")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/labels")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/plugins/cms/contentTypes"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/plugins/cms/contentmodels"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/fragments")).willReturn(aResponse().withStatus(200)));


        mockMvc.perform(post(String.format("%s/install/todomvc", URL)))
                .andDo(print()).andExpect(status().isOk());

        waitFor(5000);

        List<EntandoAppPluginLink> createdLinks = k8SServiceClientTestDouble.getInMemoryLinkCopy();
        Optional<EntandoAppPluginLink> appPluginLinkForTodoMvc = createdLinks.stream()
                .filter(link -> link.getSpec().getEntandoPluginName().equals("todomvc")).findAny();

        assertTrue(appPluginLinkForTodoMvc.isPresent());

        WireMock.verify(2, postRequestedFor(urlEqualTo("/entando-app/api/widgets")));
        WireMock.verify(2, postRequestedFor(urlEqualTo("/entando-app/api/pageModels")));
        WireMock.verify(3, postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/directory")));
        WireMock.verify(3, postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/file")));
        WireMock.verify(1, postRequestedFor(urlEqualTo("/entando-app/api/plugins/cms/contentTypes")));
        WireMock.verify(2, postRequestedFor(urlEqualTo("/entando-app/api/plugins/cms/contentmodels")));
        WireMock.verify(1, postRequestedFor(urlEqualTo("/entando-app/api/labels")));
        WireMock.verify(2, postRequestedFor(urlEqualTo("/entando-app/api/fragments")));

        final List<LoggedRequest> widgetRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/widgets")));
        final List<LoggedRequest> pageModelRequests = findAll(
                postRequestedFor(urlEqualTo("/entando-app/api/pageModels")));
        final List<LoggedRequest> directoryRequests = findAll(
                postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/directory")));
        final List<LoggedRequest> fileRequests = findAll(
                postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/file")));
        final List<LoggedRequest> contentTypeRequests = findAll(
                postRequestedFor(urlEqualTo("/entando-app/api/plugins/cms/contentTypes")));
        final List<LoggedRequest> contentModelRequests = findAll(
                postRequestedFor(urlEqualTo("/entando-app/api/plugins/cms/contentmodels")));
        final List<LoggedRequest> labelRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/labels")));
        final List<LoggedRequest> fragmentRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/fragments")));

        checkRequests(widgetRequests, pageModelRequests, directoryRequests, fileRequests, contentTypeRequests,
                contentModelRequests, labelRequests, fragmentRequests);

        widgetRequests.sort(Comparator.comparing(DigitalExchangeInstallTest::requestCode));
        pageModelRequests.sort(Comparator.comparing(DigitalExchangeInstallTest::requestCode));
        directoryRequests.sort(Comparator.comparing(DigitalExchangeInstallTest::requestPath));
        fileRequests.sort(Comparator.comparing(DigitalExchangeInstallTest::requestPath));
        fragmentRequests.sort(Comparator.comparing(DigitalExchangeInstallTest::requestCode));

        checkRequest(widgetRequests.get(0))
                .expectEqual("code", "another_todomvc_widget")
                .expectEqual("group", "free")
                .expectEqual("customUi", readFile("/bundle/widgets/widget.ftl"));

        checkRequest(widgetRequests.get(1))
                .expectEqual("code", "todomvc_widget")
                .expectEqual("group", "free")
                .expectEqual("customUi", "<h2>Bundle 1 Widget</h2>");

        checkRequest(fragmentRequests.get(0))
                .expectEqual("code", "another_fragment")
                .expectEqual("guiCode", readFile("/bundle/fragments/fragment.ftl"));

        checkRequest(fragmentRequests.get(1))
                .expectEqual("code", "title_fragment")
                .expectEqual("guiCode", "<h2>Bundle 1 Fragment</h2>");

        checkRequest(pageModelRequests.get(0))
                .expectEqual("code", "todomvc_another_page_model")
                .expectEqual("descr", "TODO MVC another page model")
                .expectEqual("group", "free")
                .expectEqual("configuration.frames[0].pos", "0")
                .expectEqual("configuration.frames[0].descr", "Simple Frame")
                .expectEqual("template", readFile("/bundle/pagemodels/page.ftl"));

        checkRequest(pageModelRequests.get(1))
                .expectEqual("code", "todomvc_page_model")
                .expectEqual("descr", "TODO MVC basic page model")
                .expectEqual("group", "free")
                .expectEqual("configuration.frames[0].pos", "0")
                .expectEqual("configuration.frames[0].descr", "Header")
                .expectEqual("configuration.frames[0].sketch.x1", "0")
                .expectEqual("configuration.frames[0].sketch.y1", "0")
                .expectEqual("configuration.frames[0].sketch.x2", "11")
                .expectEqual("configuration.frames[0].sketch.y2", "0")
                .expectEqual("configuration.frames[1].pos", "1")
                .expectEqual("configuration.frames[1].descr", "Breadcrumb")
                .expectEqual("configuration.frames[1].sketch.x1", "0")
                .expectEqual("configuration.frames[1].sketch.y1", "1")
                .expectEqual("configuration.frames[1].sketch.x2", "11")
                .expectEqual("configuration.frames[1].sketch.y2", "1");

        checkRequest(directoryRequests.get(0))
                .expectEqual("path", "/todomvc")
                .expectEqual("protectedFolder", false);

        checkRequest(directoryRequests.get(1))
                .expectEqual("path", "/todomvc/css")
                .expectEqual("protectedFolder", false);

        checkRequest(directoryRequests.get(2))
                .expectEqual("path", "/todomvc/js")
                .expectEqual("protectedFolder", false);

        checkRequest(fileRequests.get(0))
                .expectEqual("filename", "custom.css")
                .expectEqual("path", "/todomvc/css/custom.css")
                .expectEqual("base64", readFileAsBase64("/bundle/resources/css/custom.css"));

        checkRequest(fileRequests.get(1))
                .expectEqual("filename", "style.css")
                .expectEqual("path", "/todomvc/css/style.css")
                .expectEqual("base64", readFileAsBase64("/bundle/resources/css/style.css"));

        checkRequest(fileRequests.get(2))
                .expectEqual("filename", "script.js")
                .expectEqual("path", "/todomvc/js/script.js")
                .expectEqual("base64", readFileAsBase64("/bundle/resources/js/script.js"));

        WireMock.reset();

        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/widgets/todomvc_widget"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/widgets/another_todomvc_widget"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/fragments/title_fragment"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/fragments/another_fragment"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/pageModels/todomvc_page_model"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/pageModels/todomvc_another_page_model"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/api/plugins/cms/contentmodels/8880003"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/api/plugins/cms/contentmodels/8880002"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/plugins/cms/contentTypes/CNG"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/labels/HELLO")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock
                .delete(urlEqualTo("/entando-app/api/fileBrowser/directory?protectedFolder=false&currentPath=/todomvc"))
                .willReturn(aResponse().withStatus(200)));

        mockMvc.perform(get(String.format("%s/install/todomvc", URL)))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload.componentId").value("todomvc"))
                .andExpect(jsonPath("payload.status").value(JobStatus.INSTALL_COMPLETED.toString()));

        mockMvc.perform(post(String.format("%s/uninstall/todomvc", URL)))
                .andDo(print()).andExpect(status().isOk());

        waitFor(5000);

        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/widgets/todomvc_widget")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/widgets/another_todomvc_widget")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/pageModels/todomvc_page_model")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/pageModels/todomvc_another_page_model")));
        WireMock.verify(1, deleteRequestedFor(
                urlEqualTo("/entando-app/api/fileBrowser/directory?protectedFolder=false&currentPath=/todomvc")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/fragments/title_fragment")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/fragments/another_fragment")));

//        verify(mocker.operation, times(1)).delete(same(pluginMocker.plugin));
    }

    private byte[] readFromDEPackage() throws IOException {
        try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("bundle.zip")) {
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Assert.assertNotNull(inputStream);
                IOUtils.copy(inputStream, outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    @SafeVarargs
    private final void checkRequests(final List<LoggedRequest>... requests) {
        final List<LoggedRequest> allRequests = new ArrayList<>();
        for (List<LoggedRequest> request : requests) {
            allRequests.addAll(request);
        }

        for (final LoggedRequest req : allRequests) {
            assertThat(req.getHeader("Authorization")).isEqualTo("Bearer iddqd");
        }
    }

    public void waitFor(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String requestProperty(final LoggedRequest request, final String property) {
        return JsonPath.read(new String(request.getBody()), property);
    }

    private static String requestCode(final LoggedRequest request) {
        return requestProperty(request, "code");
    }

    private static String requestPath(final LoggedRequest request) {
        return requestProperty(request, "path");
    }

    private EntandoDeBundle getTestBundle() {
        return new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("todomvc")
                .withNamespace("entando-de-bundles")
                .endMetadata()
                .withSpec(getTestEntandoDeBundleSpec()).build();

    }

    private EntandoDeBundleSpec getTestEntandoDeBundleSpec() {
        return new EntandoDeBundleSpecBuilder()
                .withNewDetails()
                .withDescription("A bundle containing some demo components for Entando6")
                .withName("todomvc")
                .addNewVersion("0.0.1")
                .addNewKeyword("entando6")
                .addNewKeyword("digital-exchange")
                .addNewDistTag("latest", "0.0.1")
                .and()
                .addNewTag()
                .withVersion("0.0.1")
                .withIntegrity("sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withShasum("4d80130d7d651176953b5ce470c3a6f297a70815")
                .withTarball("http://localhost:8099/repository/npm-internal/inail_bundle/-/inail_bundle-0.0.1.tgz")
                .endTag()
                .build();
    }

}
