package org.entando.kubernetes.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


import com.github.tomakehurst.wiremock.client.WireMock;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.exception.web.HttpException;
import org.entando.kubernetes.model.bundle.descriptor.*;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.utils.EntandoCoreMockServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Arrays;

@Tag("in-process")
class EntandoCoreClientTest {

    private EntandoCoreClient client;
    private EntandoCoreMockServer coreMockServer;
    public static final String CODE = "code";

    @BeforeEach
    public void setup() {
        coreMockServer = new EntandoCoreMockServer();
        String keycloakClientId = "clientId";
        String keycloakClientSecret = "clientSecret";
        String keycloakAuthEndpoint = coreMockServer.getApiRoot() + "/auth/protocol/openid-connect/auth";
        String entandoCoreUrl = coreMockServer.getApiRoot() + "/";
        this.client = new DefaultEntandoCoreClient(keycloakClientId, keycloakClientSecret, keycloakAuthEndpoint,
                entandoCoreUrl);
    }

    @AfterEach
    public void reset() {
        coreMockServer.getInnerServer().resetAll();
        coreMockServer.stop();
    }

    @Test
    @Disabled("Can't make wiremock generic endpoint read code from url")
    void shouldWorkWithGenericComponentUsage() {
        coreMockServer = coreMockServer.withGenericComponentsUsageSupport();
        EntandoCoreComponentUsage widgetUsage = this.client.getWidgetUsage("my-new-widget");
        assertThat(widgetUsage.getCode()).isEqualTo("my-new-widget");
        assertThat(widgetUsage.getType()).isEqualTo("widgets");
        assertThat(widgetUsage.getUsage()).isEqualTo(1);

    }

    @Test
    void shouldGetUsageForWidgets() {
        coreMockServer = coreMockServer.withComponentUsageSupport(ComponentType.WIDGET, "my-widget", 11);
        EntandoCoreComponentUsage widgetUsage = this.client.getWidgetUsage("my-widget");
        assertThat(widgetUsage.getCode()).isEqualTo("my-widget");
        assertThat(widgetUsage.getType()).isEqualTo("widgets");
        assertThat(widgetUsage.getUsage()).isEqualTo(11);
    }

    @Test
    void shouldGetUsageForPage() {
        coreMockServer = coreMockServer.withComponentUsageSupport(ComponentType.PAGE, "my-page", 3);
        EntandoCoreComponentUsage widgetUsage = this.client.getPageUsage("my-page");
        assertThat(widgetUsage.getCode()).isEqualTo("my-page");
        assertThat(widgetUsage.getType()).isEqualTo("pages");
        assertThat(widgetUsage.getUsage()).isEqualTo(3);
    }

    @Test
    void shouldGetUsageForPageTemplates() {
        coreMockServer = coreMockServer.withComponentUsageSupport(ComponentType.PAGE_TEMPLATE, "my-pagemodel", 1);
        EntandoCoreComponentUsage widgetUsage = this.client.getPageModelUsage("my-pagemodel");
        assertThat(widgetUsage.getCode()).isEqualTo("my-pagemodel");
        assertThat(widgetUsage.getType()).isEqualTo("pageModels");
        assertThat(widgetUsage.getUsage()).isEqualTo(1);
    }

    @Test
    void shouldGetUsageForFragments() {
        coreMockServer = coreMockServer.withComponentUsageSupport(ComponentType.FRAGMENT, "fragment-101", 1);
        EntandoCoreComponentUsage widgetUsage = this.client.getFragmentUsage("fragment-101");
        assertThat(widgetUsage.getCode()).isEqualTo("fragment-101");
        assertThat(widgetUsage.getType()).isEqualTo("fragments");
        assertThat(widgetUsage.getUsage()).isEqualTo(1);
    }

    @Test
    void shouldGetUsageForContentTypes() {
        coreMockServer = coreMockServer.withComponentUsageSupport(ComponentType.CONTENT_TYPE, "CT092", 2);
        EntandoCoreComponentUsage widgetUsage = this.client.getContentTypeUsage("CT092");
        assertThat(widgetUsage.getCode()).isEqualTo("CT092");
        assertThat(widgetUsage.getType()).isEqualTo("contentTypes");
        assertThat(widgetUsage.getUsage()).isEqualTo(2);
    }

    @Test
    void shouldGetUsageForContentTemplate() {
        coreMockServer = coreMockServer.withComponentUsageSupport(ComponentType.CONTENT_TEMPLATE, "12345", 8);
        EntandoCoreComponentUsage contentModelUsage = this.client.getContentModelUsage("12345");
        assertThat(contentModelUsage.getCode()).isEqualTo("12345");
        assertThat(contentModelUsage.getType()).isEqualTo("contentTemplates");
        assertThat(contentModelUsage.getUsage()).isEqualTo(8);
    }

    @Test
    void getUsageReceiving3xxStatusCodeShouldBeManagedInTheCoreClient() {
        coreMockServer = coreMockServer.withFailingComponentUsageSupport(ComponentType.CONTENT_TEMPLATE, "12345", HttpStatus.NOT_MODIFIED);
        assertThrows(HttpException.class, () -> this.client.getContentModelUsage("12345"));
    }

    @Test
    void getUsageReceiving4xxStatusCodeShouldThrowAndExceptionByRestTemplateItself() {
        coreMockServer = coreMockServer.withFailingComponentUsageSupport(ComponentType.CONTENT_TEMPLATE, "12345", HttpStatus.NOT_FOUND);
        assertThrows(HttpClientErrorException.NotFound.class, () -> this.client.getContentModelUsage("12345"));
    }


    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void registerWidget() {
        WidgetDescriptor wd = new WidgetDescriptor();
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.WIDGET_ENDPOINT, WireMock::post);
        this.client.registerWidget(wd);
    }


    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void deleteWidget() {
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.WIDGET_ENDPOINT, CODE, WireMock::delete);
        this.client.deleteWidget(CODE);
    }

    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void registerFragment() {
        FragmentDescriptor fd = new FragmentDescriptor();
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.FRAGMENT_ENDPOINT, WireMock::post);
        this.client.registerFragment(fd);
    }


    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void deleteFragment() {
        WidgetDescriptor wd = new WidgetDescriptor();
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.FRAGMENT_ENDPOINT, CODE, WireMock::delete);
        this.client.deleteFragment(CODE);
    }

    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void registerLabel() {
        LabelDescriptor ld = new LabelDescriptor();
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.LABEL_ENDPOINT, WireMock::post);
        this.client.registerLabel(ld);
    }


    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void deleteLabel() {
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.LABEL_ENDPOINT, CODE, WireMock::delete);
        this.client.deleteLabel(CODE);
    }

    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void registerPage() {
        PageDescriptor pd = new PageDescriptor();
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.PAGE_ENDPOINT, WireMock::post);
        this.client.registerPage(pd);
    }


    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void deletePage() {
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.PAGE_ENDPOINT, CODE, WireMock::delete);
        this.client.deletePage(CODE);
    }

    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void registerPageTemplate() {

        PageTemplateConfigurationDescriptor pageTemplateConfigurationDescriptor = PageTemplateConfigurationDescriptor.builder()
                .frames(Arrays.asList(new FrameDescriptor()))
                .build();
        PageTemplateDescriptor ptd = PageTemplateDescriptor.builder()
                .configuration(pageTemplateConfigurationDescriptor)
                .build();

        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.PAGE_TEMPLATE_ENDPOINT, WireMock::post);
        this.client.registerPageModel(ptd);
    }


    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void deletePageTemplate() {
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.PAGE_TEMPLATE_ENDPOINT, CODE, WireMock::delete);
        this.client.deletePageModel(CODE);
    }

    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void registerContentTemplate() {
        ContentTemplateDescriptor ctd = new ContentTemplateDescriptor();
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.CONTENT_TEMPLATE_ENDPOINT, WireMock::post);
        this.client.registerContentModel(ctd);
    }


    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void deleteContentTemplate() {
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.CONTENT_TEMPLATE_ENDPOINT, CODE, WireMock::delete);
        this.client.deleteContentModel(CODE);
    }

    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void registerContentType() {
        ContentTypeDescriptor ctd = new ContentTypeDescriptor();
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.CONTENT_TYPE_ENDPOINT, WireMock::post);
        this.client.registerContentType(ctd);
    }


    // TODO added because sonar quality blocks the merge. refine this test as soon as possible
    @Test
    void deleteContentType() {
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.CONTENT_TYPE_ENDPOINT, CODE, WireMock::delete);
        this.client.deleteContentType(CODE);
    }
}
