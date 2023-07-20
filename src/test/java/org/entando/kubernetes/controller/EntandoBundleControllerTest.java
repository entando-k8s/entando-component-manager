package org.entando.kubernetes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.TestEntitiesGenerator.getTestEntandoBundle;
import static org.entando.kubernetes.TestEntitiesGenerator.getTestJobEntity;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.component.EntandoBundleResourceController;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsageRequest;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.security.AuthorizationChecker;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentUsageService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

@Tag("in-process")
public class EntandoBundleControllerTest {

    private EntandoBundleResourceController controller;
    private EntandoBundleComponentUsageService usageService;
    private EntandoCoreClient coreClient;
    private EntandoBundleService bundleService;
    private AuthorizationChecker authorizationChecker;

    @BeforeEach
    public void setup() {
        bundleService = mock(EntandoBundleServiceImpl.class);
        coreClient = mock(EntandoCoreClient.class);
        authorizationChecker = mock(AuthorizationChecker.class);
        usageService = new EntandoBundleComponentUsageService(coreClient);
        controller = new EntandoBundleResourceController(bundleService, usageService, authorizationChecker);
    }

    @Test
    public void shouldThrowBadRequestExceptionWhenGettingSummaryOfNotInstalledComponent() {
        when(bundleService.getInstalledBundle(any())).thenReturn(Optional.empty());
        when(bundleService.getBundleInstalledComponents(any())).thenCallRealMethod();
        assertThrows(BundleNotInstalledException.class, () -> controller.getBundleUsageSummary("any"));
    }

    @Test
    public void shouldThrowIfInstalledBundleDoesntHaveAssociatedCompletedJob() {
        EntandoBundle bundle = getTestEntandoBundle();
        when(bundleService.getBundleInstalledComponents(any())).thenCallRealMethod();
        when(bundleService.getInstalledBundle(any())).thenReturn(Optional.of(bundle));

        assertThrows(EntandoComponentManagerException.class, () -> controller.getBundleUsageSummary("any"));
    }

    @Test
    public void shouldReturnEmptySummaryIfBundleHasNoComponents() {
        EntandoBundle bundle = getTestEntandoBundle();
        when(bundleService.getInstalledBundle(any())).thenReturn(Optional.of(bundle));

        ResponseEntity<SimpleRestResponse<List<ComponentUsage>>> resp = controller
                .getBundleUsageSummary("my-component");
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(Objects.requireNonNull(resp.getBody()).getPayload()).isEmpty();
    }

    @Test
    public void shouldReturnEmptySummaryIfBundleHasIrrelevantComponents() {
        EntandoBundleJobEntity job = getTestJobEntity();

        EntandoBundleComponentJobEntity componentJob = new EntandoBundleComponentJobEntity();
        componentJob.setStatus(JobStatus.INSTALL_COMPLETED);
        componentJob.setParentJob(job);
        componentJob.setComponentId("my-magic-resource");
        componentJob.setComponentType(ComponentType.RESOURCE);

        EntandoBundle component = getTestEntandoBundle();

        when(bundleService.getInstalledBundle(any())).thenReturn(Optional.of(component));
        when(bundleService.getBundleInstalledComponents(any())).thenReturn(Collections.singletonList(componentJob));

        ResponseEntity<SimpleRestResponse<List<ComponentUsage>>> resp = controller
                .getBundleUsageSummary("my-component");
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(Objects.requireNonNull(resp.getBody()).getPayload()).isEmpty();
    }

    @Test
    public void shouldReturnSummaryForRelevantComponents() {
        EntandoBundleJobEntity job = getTestJobEntity();

        EntandoBundleComponentJobEntity cjA = new EntandoBundleComponentJobEntity();
        cjA.setStatus(JobStatus.INSTALL_COMPLETED);
        cjA.setParentJob(job);
        cjA.setComponentId("my-magic-resource");
        cjA.setComponentType(ComponentType.RESOURCE);

        EntandoBundleComponentJobEntity cjB = new EntandoBundleComponentJobEntity();
        cjB.setStatus(JobStatus.INSTALL_COMPLETED);
        cjB.setParentJob(job);
        cjB.setComponentId("my-magic-widget");
        cjB.setComponentType(ComponentType.WIDGET);

        EntandoBundleComponentJobEntity cjC = new EntandoBundleComponentJobEntity();
        cjC.setStatus(JobStatus.INSTALL_COMPLETED);
        cjC.setParentJob(job);
        cjC.setComponentId("my-magic-page");
        cjC.setComponentType(ComponentType.PAGE);

        EntandoBundle component = getTestEntandoBundle();

        when(bundleService.getInstalledBundle(any())).thenReturn(Optional.of(component));
        when(bundleService.getBundleInstalledComponents(any())).thenReturn(Arrays.asList(cjA, cjB, cjC));

        when(coreClient.getComponentsUsageDetails(Arrays.asList(
                new EntandoCoreComponentUsageRequest(ComponentType.WIDGET, "my-magic-widget"),
                new EntandoCoreComponentUsageRequest(ComponentType.PAGE, "my-magic-page")
        )))
                .thenReturn(Arrays.asList(
                        new EntandoCoreComponentUsage(ComponentType.WIDGET, "my-magic-widget",
                                true, 11, Collections.emptyList()),
                        new EntandoCoreComponentUsage(ComponentType.PAGE, "my-magic-page",
                                true, 5,
                                Collections.emptyList())
                ));

        ResponseEntity<SimpleRestResponse<List<ComponentUsage>>> resp = controller
                .getBundleUsageSummary("my-component");
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        List<ComponentUsage> usageList = resp.getBody().getPayload();

        assertThat(usageList).hasSize(2);
        assertThat(usageList.stream()
                .filter(usc -> usc.getType().equals(ComponentType.WIDGET) && usc.getCode()
                        .equals("my-magic-widget"))
                .findFirst().get().getUsage()).isEqualTo(11);
        assertThat(usageList.stream()
                .filter(usc -> usc.getType().equals(ComponentType.PAGE) && usc.getCode()
                        .equals("my-magic-page"))
                .findFirst().get().getUsage()).isEqualTo(5);
        assertThat(usageList.stream().map(ComponentUsage::getType).distinct().count())
                .isEqualTo(2);
        assertThat(usageList.stream().map(ComponentUsage::getUsage).reduce(0, Integer::sum))
                .isEqualTo(16);
    }

    @Test
    public void shouldProvideSummaryForDifferentWidgets() {
        EntandoBundleJobEntity job = getTestJobEntity();

        EntandoBundleComponentJobEntity cjA = new EntandoBundleComponentJobEntity();
        cjA.setStatus(JobStatus.INSTALL_COMPLETED);
        cjA.setParentJob(job);
        cjA.setComponentId("my-magic-resource");
        cjA.setComponentType(ComponentType.RESOURCE);

        EntandoBundleComponentJobEntity cjB = new EntandoBundleComponentJobEntity();
        cjB.setStatus(JobStatus.INSTALL_COMPLETED);
        cjB.setParentJob(job);
        cjB.setComponentId("my-magic-widget");
        cjB.setComponentType(ComponentType.WIDGET);

        EntandoBundleComponentJobEntity cjC = new EntandoBundleComponentJobEntity();
        cjC.setStatus(JobStatus.INSTALL_COMPLETED);
        cjC.setParentJob(job);
        cjC.setComponentId("my-other-widget");
        cjC.setComponentType(ComponentType.WIDGET);

        EntandoBundle component = getTestEntandoBundle();

        when(bundleService.getInstalledBundle(any())).thenReturn(Optional.of(component));
        when(bundleService.getBundleInstalledComponents(any())).thenReturn(Arrays.asList(cjA, cjB, cjC));

        when(coreClient.getComponentsUsageDetails(Arrays.asList(
                new EntandoCoreComponentUsageRequest(ComponentType.WIDGET, "my-magic-widget"),
                new EntandoCoreComponentUsageRequest(ComponentType.WIDGET, "my-other-widget")
        )))
                .thenReturn(Arrays.asList(
                        new EntandoCoreComponentUsage(ComponentType.WIDGET, "my-magic-widget",
                                true, 11, Collections.emptyList()),
                        new EntandoCoreComponentUsage(ComponentType.WIDGET, "my-other-widget",
                                true, 5, Collections.emptyList())
                ));

        ResponseEntity<SimpleRestResponse<List<ComponentUsage>>> resp = controller
                .getBundleUsageSummary("my-component");
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        List<ComponentUsage> usageList = resp.getBody().getPayload();

        assertThat(usageList).hasSize(2);
        assertThat(usageList.stream()
                .filter(usc -> usc.getType().equals(ComponentType.WIDGET) && usc.getCode()
                        .equals("my-magic-widget"))
                .findFirst()
                .get().getUsage()).isEqualTo(11);
        assertThat(usageList.stream()
                .filter(usc -> usc.getType().equals(ComponentType.WIDGET) && usc.getCode()
                        .equals("my-other-widget"))
                .findFirst()
                .get()
                .getUsage())
                .isEqualTo(5);

    }

}
