package org.entando.kubernetes.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;

@Slf4j
public class EntandoHubMockServer extends EntandoGenericMockServer {

    public EntandoHubMockServer() {
        super();
    }

    @Override
    protected void init(WireMockServer wireMockServer) {
        addBundle(wireMockServer);
        addBundleGroup(wireMockServer);
    }

    private void addBundleGroup(WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlEqualTo("/appbuilder/api/bundlegroups/?page=1&descriptorVersions=v5&descriptorVersions=v1&pageSize=1"))
                // not correctly handled so far https://github.com/wiremock/wiremock/issues/398
//                .withQueryParam("page", WireMock.equalTo("1"))
//                .withQueryParam("descriptorVersions", WireMock.equalTo("v5"))
//                .withQueryParam("descriptorVersions", WireMock.equalTo("v1"))
//                .withQueryParam("pageSize", WireMock.equalTo("1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(BUNDLEGROUP_RESPONSE_JSON)));
    }

    private void addBundle(WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlEqualTo("/appbuilder/api/bundles/?descriptorVersions=v1&descriptorVersions=v5&pageSize=1&page=1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(BUNDLE_RESPONSE_JSON)));
    }

    public static final String BUNDLEGROUP_RESPONSE_JSON = "{\"payload\":[{\"bundleGroupId\":1,\"bundleGroupVersionId\":4,\"name\":\"Bundle\",\"description\":\"Description for version #2\",\"descriptionImage\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAACXBIWXMAAAsTAAALEwEAmpwYAAAA1ElEQVR4nL3SMU5CQRDG8V/lASisbey08QoW2NEQsTB23MHQYqN0XoAeChKijdET2HAAQmEs0RoKo9lkJS+bfS8PEv0n0+x+M/PN7PLPXOETT9hLL0+xxHcmAud4wzEWOEkLTHFZ0f0eh7jDa87BOw5KbM8wwRDPaKSifXxkktvRduh8i6Mye81YOeUBXQwS22GJZ0XhdZwtpRW3/pLYDm56ReEIF+rTwbh4MC95vqoIORu+cputoBFzNvx+ljo85nLWOzhYFQ9uYpG68wdtf4uGf8gPOJ9BPZZIWrEAAAAASUVORK5CYII=\",\"documentationUrl\":\"http://docm.me\",\"version\":\"v0.0.2\",\"status\":\"PUBLISHED\",\"organisationId\":1,\"organisationName\":\"Entando\",\"publicCatalog\":true,\"categories\":[\"3\"],\"children\":[\"2\"],\"allVersions\":[\"v0.0.2\",\"v0.0.1\"],\"createdAt\":null,\"lastUpdate\":\"2023-03-06T08:53:32.120095\",\"bundleGroupUrl\":\"http://localhost:3000/#/bundlegroup/versions/4\",\"isEditable\":false,\"canAddNewVersion\":true,\"displayContactUrl\":true,\"contactUrl\":\"http://contact.me\"}],\"metadata\":{\"page\":1,\"pageSize\":1,\"lastPage\":1,\"totalItems\":1}}";
    public static final String BUNDLE_RESPONSE_JSON = "{\"payload\":[{\"bundleId\":\"13\",\"name\":\"bundle-uri-1\",\"description\":\"Description default\",\"descriptionImage\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAACXBIWXMAAAsTAAALEwEAmpwYAAAA1ElEQVR4nL3SMU5CQRDG8V/lASisbey08QoW2NEQsTB23MHQYqN0XoAeChKijdET2HAAQmEs0RoKo9lkJS+bfS8PEv0n0+x+M/PN7PLPXOETT9hLL0+xxHcmAud4wzEWOEkLTHFZ0f0eh7jDa87BOw5KbM8wwRDPaKSifXxkktvRduh8i6Mye81YOeUBXQwS22GJZ0XhdZwtpRW3/pLYDm56ReEIF+rTwbh4MC95vqoIORu+cputoBFzNvx+ljo85nLWOzhYFQ9uYpG68wdtf4uGf8gPOJ9BPZZIWrEAAAAASUVORK5CYII=\",\"descriptorVersion\":\"V1\",\"gitRepoAddress\":\"https://github.com/account/bundle-uri-1.git\",\"gitSrcRepoAddress\":\"https://github.com/account/source-url-1.git\",\"dependencies\":[\"\"],\"bundleGroups\":[\"15\"]}],\"metadata\":{\"page\":1,\"pageSize\":1,\"lastPage\":7,\"totalItems\":7}}";
}
