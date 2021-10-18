package org.entando.kubernetes.assertionhelper;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.ObjectUtils;
import org.assertj.core.api.Java6Assertions;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistryEntity;
import org.entando.kubernetes.stubhelper.EntandoHubRegistryStubHelper;
import org.hamcrest.core.IsNull;
import org.springframework.test.web.servlet.ResultActions;

public class EntandoHubRegistryAssertionHelper {

    public static void assertOnEntandoHubRegistries(EntandoHubRegistry current, EntandoHubRegistryEntity expected) {
        assertOnEntandoHubRegistry(current,
                expected.getId().toString(),
                expected.getName(),
                expected.getUrl().toString());
    }

    public static void assertOnEntandoHubRegistry(EntandoHubRegistry registry) {
        assertOnEntandoHubRegistry(registry,
                EntandoHubRegistryStubHelper.REGISTRY_ID_1,
                EntandoHubRegistryStubHelper.REGISTRY_NAME_1,
                EntandoHubRegistryStubHelper.REGISTRY_URL_STRING_1);
    }

    public static void assertOnEntandoHubRegistry(EntandoHubRegistry registry, String id, String name, String url) {
        Java6Assertions.assertThat(registry.getId()).isEqualTo(id);
        Java6Assertions.assertThat(registry.getName()).isEqualTo(name);
        Java6Assertions.assertThat(registry.getUrl().toString()).isEqualTo(url);
    }


    public static void assertOnEntandoHubRegistryEntity(EntandoHubRegistryEntity entity) {
        assertOnEntandoHubRegistryEntity(entity,
                EntandoHubRegistryStubHelper.REGISTRY_ID_1,
                EntandoHubRegistryStubHelper.REGISTRY_NAME_1,
                EntandoHubRegistryStubHelper.REGISTRY_URL_STRING_1);
    }

    public static void assertOnEntandoHubRegistryEntity(EntandoHubRegistryEntity entity, String id, String name,
            String url) {

        if (ObjectUtils.isEmpty(id)) {
            Java6Assertions.assertThat(entity.getId()).isNull();
        } else {
            Java6Assertions.assertThat(entity.getId().toString()).isEqualTo(id);
        }
        Java6Assertions.assertThat(entity.getName()).isEqualTo(name);
        Java6Assertions.assertThat(entity.getUrl().toString()).isEqualTo(url);
    }


    public static void assertOnEntandoHubRegistryEntity(ResultActions result, Integer index,
            EntandoHubRegistry registry) throws Exception {

        String baseJsonPath = Optional.ofNullable(index)
                .map(i -> "$.payload.[" + i + "].")
                .orElse("$.payload.");

        result.andExpect(jsonPath(baseJsonPath + "id", IsNull.notNullValue()))
                .andExpect(jsonPath(baseJsonPath + "name", is(registry.getName())))
                .andExpect(jsonPath(baseJsonPath + "url", is(registry.getUrl().toString())));
    }


    public static void assertOnEntandoHubRegistryEntityList(ResultActions result, List<EntandoHubRegistry> registryList) throws Exception {

        result.andExpect(status().isOk());

        for (int i = 0; i < registryList.size(); i++) {
            assertOnEntandoHubRegistryEntity(result, i, registryList.get(i));
        }
    }
}
