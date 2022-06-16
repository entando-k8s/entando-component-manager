package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.job.EntandoBundleComponentJob;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentServiceImpl;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("unit")
class EntandoBundleComponentServiceTest {

    private EntandoBundleComponentService service;
    private EntandoBundleService bundleService;
    private EntandoBundleComponentJobRepository componentJobRepository;

    @BeforeEach
    public void setup() {
        componentJobRepository = Mockito.mock(EntandoBundleComponentJobRepository.class);
        bundleService = Mockito.mock(EntandoBundleService.class);
        service = new EntandoBundleComponentServiceImpl(componentJobRepository, bundleService);
    }

    @AfterEach
    public void teardown() {
    }

    @Test
    void getInstalledComponentsByBundleCode_withValidBundleCode_shouldReturnBundleComponentList() {

        final PagedListRequest req = new PagedListRequest();

        final Optional<EntandoBundle> installedBundle = Optional.of(
                TestEntitiesGenerator.getTestEntandoBundle(TestEntitiesGenerator.getTestJob()));
        when(bundleService.getInstalledBundle(any())).thenReturn(installedBundle);

        List<EntandoBundleComponentJobEntity> allComponents =
                Collections.singletonList(new EntandoBundleComponentJobEntity());
        when(componentJobRepository.findAllByParentJobId(any())).thenReturn(allComponents);

        PagedMetadata<EntandoBundleComponentJob> components = service.getInstalledComponentsByBundleCode(req,
                installedBundle.get().getCode());

        assertThat(components.getBody().size()).isEqualTo(1);

        when(bundleService.getInstalledBundle(any())).thenReturn(Optional.empty());

        assertThrows(BundleNotInstalledException.class,
                () -> service.getInstalledComponentsByBundleCode(req, installedBundle.get().getCode()));

    }

    @Test
    void getInstalledComponentsByBundleCode_withInvalidBundleCode_shouldReturnError() {

        final PagedListRequest req = new PagedListRequest();
        when(bundleService.getInstalledBundle(any())).thenReturn(Optional.empty());

        assertThrows(BundleNotInstalledException.class, () -> service.getInstalledComponentsByBundleCode(req, ""));
    }

    @Test
    void getInstalledComponentsByEncodedUrl_withValidEncoded_shouldReturnBundleComponentList() {

        final PagedListRequest req = new PagedListRequest();

        final Optional<EntandoBundle> installedBundle = Optional.of(
                TestEntitiesGenerator.getTestEntandoBundle(TestEntitiesGenerator.getTestJob()));
        when(bundleService.getInstalledBundleByEncodedUrl(any())).thenReturn(installedBundle);
        when(bundleService.getInstalledBundle(any())).thenReturn(installedBundle);

        List<EntandoBundleComponentJobEntity> allComponents =
                Collections.singletonList(new EntandoBundleComponentJobEntity());
        when(componentJobRepository.findAllByParentJobId(any())).thenReturn(allComponents);

        PagedMetadata<EntandoBundleComponentJob> components = components = service.getInstalledComponentsByEncodedUrl(
                req, installedBundle.get().getCode());
        assertThat(components.getBody().size()).isEqualTo(1);
    }

    @Test
    void getInstalledComponentsByEncodedUrl_withInvalidEncoded_shouldReturnError() {

        final PagedListRequest req = new PagedListRequest();

        when(bundleService.getInstalledBundleByEncodedUrl(any())).thenReturn(Optional.empty());

        assertThrows(BundleNotInstalledException.class, () -> service.getInstalledComponentsByBundleCode(req, ""));
    }

}
