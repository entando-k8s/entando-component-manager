package org.entando.kubernetes.model.assembler;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.assertionhelper.EntandoHubRegistryAssertionHelper;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistryEntity;
import org.entando.kubernetes.stubhelper.EntandoHubRegistryStubHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class EntandoHubRegistryAssemblerTest {

    @Test
    void shouldThrowExceptionWhenConvertingANullDto() {
        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> EntandoHubRegistryAssembler.toEntandoHubRegistryEntity(null));
    }

    @Test
    void shouldCorrectlyConvertAFullyPopulateDtoToEntity() {
        final EntandoHubRegistryEntity entity = EntandoHubRegistryAssembler.toEntandoHubRegistryEntity(
                EntandoHubRegistryStubHelper.stubEntandoHubRegistry1());

        EntandoHubRegistryAssertionHelper.assertOnEntandoHubRegistryEntity(entity);
    }

    @Test
    void shouldThrowExceptionWhenConvertingANullEntity() {
        Assertions.assertThrows(EntandoComponentManagerException.class,
                () -> EntandoHubRegistryAssembler.toEntandoHubRegistry(null));
    }

    @Test
    void shouldCorrectlyConvertToAListOfEntandoHubRegistryEntity() {

        final List<EntandoHubRegistry> entandoHubRegistries = EntandoHubRegistryAssembler.toListOfEntandoHubRegistry(
                EntandoHubRegistryStubHelper.stubListOfEntandoHubRegistryEntity());

        EntandoHubRegistryAssertionHelper.assertOnEntandoHubRegistry(entandoHubRegistries.get(0));
        EntandoHubRegistryAssertionHelper.assertOnEntandoHubRegistry(entandoHubRegistries.get(1),
                EntandoHubRegistryStubHelper.REGISTRY_ID_2,
                EntandoHubRegistryStubHelper.REGISTRY_NAME_2,
                EntandoHubRegistryStubHelper.REGISTRY_URL_STRING_2);
    }

    @Test
    void shouldNOTThrowExceptionWhenConvertingAnEmptyListOfEntandoHubRegistryEntity() {

        assertThat(EntandoHubRegistryAssembler.toListOfEntandoHubRegistry(Collections.emptyList())).hasSize(0);
    }
}
