package org.entando.kubernetes.service.digitalexchange.entandohub;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.entando.kubernetes.assertionhelper.EntandoHubRegistryAssertionHelper;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.web.NotFoundExceptionWeb;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistryEntity;
import org.entando.kubernetes.repository.EntandoHubRegistryRepository;
import org.entando.kubernetes.stubhelper.EntandoHubRegistryStubHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EntandoHubRegistryServiceImplTest {

    @Mock
    private EntandoHubRegistryRepository repository;

    private EntandoHubRegistryServiceImpl service;

    @BeforeEach
    public void setup() {
        this.service = new EntandoHubRegistryServiceImpl(repository);
    }

    @Test
    void shouldReturnTheExpectedListOfEntandoHubRegistry() {
        when(repository.findAllByOrderByNameAsc()).thenReturn(
                EntandoHubRegistryStubHelper.stubListOfEntandoHubRegistryEntity());

        final List<EntandoHubRegistry> current = this.service.listRegistries();
        final List<EntandoHubRegistry> expected = EntandoHubRegistryStubHelper.stubListOfEntandoHubRegistry();
        assertThat(current).containsExactlyInAnyOrder(expected.toArray(new EntandoHubRegistry[expected.size()]));
    }

    @Test
    void shouldReturnTheExpectedEntandoHubRegistryOnCreateRegistry() {

        EntandoHubRegistryEntity registryToSave = EntandoHubRegistryStubHelper.stubEntandoHubRegistryEntity1();

        when(repository.findByName(registryToSave.getName())).thenReturn(
                Optional.empty());
        when(repository.findByUrl(registryToSave.getUrl())).thenReturn(
                Optional.empty());
        when(repository.save(registryToSave)).thenReturn(registryToSave);

        final EntandoHubRegistry current = this.service.createRegistry(
                EntandoHubRegistryStubHelper.stubEntandoHubRegistry1());

        EntandoHubRegistryAssertionHelper.assertOnEntandoHubRegistries(current, registryToSave);
    }

    @Test
    void shouldReturnErrorWhenCreatingANewHubRegistryTryingToUseAnExistingNameOrUrl() {

        EntandoHubRegistry registryToSave = EntandoHubRegistryStubHelper.stubEntandoHubRegistry1();

        when(repository.findByName(any())).thenReturn(Optional.of(new EntandoHubRegistryEntity()));
        Assertions.assertThrows(EntandoComponentManagerException.class, () -> service.createRegistry(registryToSave));

        reset(repository);
        when(repository.findByUrl(any())).thenReturn(Optional.of(new EntandoHubRegistryEntity()));
        Assertions.assertThrows(EntandoComponentManagerException.class, () -> service.createRegistry(registryToSave));
    }

    @Test
    void shouldReturnTheExpectedEntandoHubRegistryOnUpdateExistingRegistry() {

        EntandoHubRegistryEntity registryToSave = EntandoHubRegistryStubHelper.stubEntandoHubRegistryEntity1();

        when(repository.findByNameAndIdNot(registryToSave.getName(), registryToSave.getId())).thenReturn(
                Optional.empty());
        when(repository.findByUrlAndIdNot(registryToSave.getUrl(), registryToSave.getId())).thenReturn(
                Optional.empty());
        when(repository.findById(registryToSave.getId())).thenReturn(Optional.of(registryToSave));
        when(repository.save(registryToSave)).thenReturn(registryToSave);

        final EntandoHubRegistry current = this.service.updateRegistry(
                EntandoHubRegistryStubHelper.stubEntandoHubRegistry1());

        EntandoHubRegistryAssertionHelper.assertOnEntandoHubRegistries(current, registryToSave);
    }

    @Test
    void shouldReturnErrorWhenUpdatingAnEntandoHubRegistryTryingToSetAnExistingNameOrUrlOnDifferentEntity() {

        EntandoHubRegistry registryToSave = EntandoHubRegistryStubHelper.stubEntandoHubRegistry1();

        when(repository.findByNameAndIdNot(any(), any())).thenReturn(Optional.of(new EntandoHubRegistryEntity()));
        Assertions.assertThrows(EntandoComponentManagerException.class, () -> service.updateRegistry(registryToSave));

        reset(repository);
        when(repository.findByUrlAndIdNot(any(), any())).thenReturn(Optional.of(new EntandoHubRegistryEntity()));
        Assertions.assertThrows(EntandoComponentManagerException.class, () -> service.updateRegistry(registryToSave));
    }

    @Test
    void shouldThrowExceptionOnUpdateNotFoundRegistry() {

        EntandoHubRegistryEntity registryToSave = EntandoHubRegistryStubHelper.stubEntandoHubRegistryEntity1();

        when(repository.findById(registryToSave.getId())).thenReturn(Optional.empty());

        final EntandoHubRegistry entandoHubRegistry = EntandoHubRegistryStubHelper.stubEntandoHubRegistry1();
        Assertions.assertThrows(NotFoundExceptionWeb.class, () -> this.service.updateRegistry(entandoHubRegistry));
    }

    @Test
    void shouldReturnTheNameOfTheDeletedRegistryOnRegistryDeletion() {
        final Optional<EntandoHubRegistryEntity> registryOpt = Optional.of(
                EntandoHubRegistryStubHelper.stubEntandoHubRegistryEntity1());
        when(repository.findById(UUID.fromString(EntandoHubRegistryStubHelper.REGISTRY_ID_1))).thenReturn(registryOpt);
        doNothing().when(repository).delete(any());

        final String name = service.deleteRegistry(EntandoHubRegistryStubHelper.REGISTRY_ID_1);
        verify(repository).delete(any());
        assertThat(name).isEqualTo(registryOpt.get().getName());
    }

    @Test
    void shouldReturnEmptyStringOnRegistryDeletionIfRegistryNotFound() {
        when(repository.findById(UUID.fromString(EntandoHubRegistryStubHelper.REGISTRY_ID_1))).thenReturn(
                Optional.empty());

        final String name = service.deleteRegistry(EntandoHubRegistryStubHelper.REGISTRY_ID_1);
        verify(repository, times(0)).delete(any());
        assertThat(name).isEmpty();
    }
}
