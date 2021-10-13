package org.entando.kubernetes.stubhelper;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistryEntity;

public class EntandoHubRegistryStubHelper {

    public static final String REGISTRY_ID_1 = "69e4a917-ad6a-4965-9aa5-c7e5a5914d0a";
    public static final String REGISTRY_NAME_1 = "name_1";
    public static final String REGISTRY_URL_STRING_1 = "http://www.entando.com/registry_1";
    public static final String REGISTRY_ID_2 = "69e4a917-ad6a-4965-9aa5-c7e5a5914d0b";
    public static final String REGISTRY_NAME_2 = "name_2";
    public static final String REGISTRY_URL_STRING_2 = "http://www.entando.com/registry_2";
    public static final String REGISTRY_ID_3 = "69e4a917-ad6a-4965-9aa5-c7e5a5914d0c";
    public static final String REGISTRY_NAME_3 = "name_3";
    public static final String REGISTRY_URL_STRING_3 = "http://www.entando.com/registry_3";

    @SneakyThrows
    public static EntandoHubRegistry stubEntandoHubRegistry1() {
        return new EntandoHubRegistry()
                .setId(REGISTRY_ID_1)
                .setName(REGISTRY_NAME_1)
                .setUrl(new URL(REGISTRY_URL_STRING_1));
    }

    @SneakyThrows
    public static EntandoHubRegistry stubEntandoHubRegistry2() {
        return new EntandoHubRegistry()
                .setId(REGISTRY_ID_2)
                .setName(REGISTRY_NAME_2)
                .setUrl(new URL(REGISTRY_URL_STRING_2));
    }

    @SneakyThrows
    public static EntandoHubRegistry stubEntandoHubRegistry3() {
        return new EntandoHubRegistry()
                .setId(REGISTRY_ID_3)
                .setName(REGISTRY_NAME_3)
                .setUrl(new URL(REGISTRY_URL_STRING_3));
    }

    public static List<EntandoHubRegistry> stubListOfEntandoHubRegistry() {
        return Arrays.asList(stubEntandoHubRegistry1(), stubEntandoHubRegistry2());
    }

    @SneakyThrows
    public static EntandoHubRegistryEntity stubEntandoHubRegistryEntity1() {
        return new EntandoHubRegistryEntity()
                .setId(UUID.fromString(REGISTRY_ID_1))
                .setName(REGISTRY_NAME_1)
                .setUrl(new URL(REGISTRY_URL_STRING_1));
    }

    @SneakyThrows
    public static EntandoHubRegistryEntity stubEntandoHubRegistryEntity2() {
        return new EntandoHubRegistryEntity()
                .setId(UUID.fromString(REGISTRY_ID_2))
                .setName(REGISTRY_NAME_2)
                .setUrl(new URL(REGISTRY_URL_STRING_2));
    }

    public static List<EntandoHubRegistryEntity> stubListOfEntandoHubRegistryEntity() {
        return Arrays.asList(stubEntandoHubRegistryEntity1(), stubEntandoHubRegistryEntity2());
    }
}
