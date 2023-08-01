package org.entando.kubernetes.controller.digitalexchange.component;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.KubernetesClientException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.assertionhelper.SimpleRestResponseAssertionHelper;
import org.entando.kubernetes.config.tenant.TenantContextManager;
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;
import org.entando.kubernetes.model.bundle.status.BundlesStatusQuery;
import org.entando.kubernetes.model.bundle.status.BundlesStatusResult;
import org.entando.kubernetes.model.common.RestNamedId;
import org.entando.kubernetes.model.web.response.DeletedObjectResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.security.AuthorizationChecker;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStatusItemStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zalando.problem.DefaultProblem;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EntandoBundleResourceControllerTest {

    private EntandoBundleResourceController controller;

    @Mock
    private EntandoBundleService bundleService;
    @Mock
    private AuthorizationChecker authorizationChecker;

    @Mock
    private TenantContextManager tenantContextManager;

    @BeforeEach
    public void setup() {
        controller = new EntandoBundleResourceController(bundleService, null,
                authorizationChecker, tenantContextManager);
    }


    @Test
    void shouldReturnTheExpectedResponseWhenUnsuccessfullyDeployAnEntandoDeBundle() {

        // given that the user wants to deploy a new EntandoDeBundle and that the k8s service answer with a KO
        when(bundleService.deployDeBundle(any())).thenThrow(new KubernetesClientException("error"));

        // when the user sends the request
        // then a KubernetesClientException is thrown
        assertThrows(KubernetesClientException.class, () -> controller.deployBundle("jwt", null));
    }

    @Test
    void shouldReturnTheExpectedResponseWhenSuccessfullyDeployAnEntandoDeBundle() {

        // given that the user wants to deploy a new EntandoDeBundle and that the k8s service answer with an OK
        final EntandoBundle expected = TestEntitiesGenerator.getTestEntandoBundle();
        when(bundleService.deployDeBundle(any())).thenReturn(expected);
        final BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo();

        // when the user sends the request
        final ResponseEntity<SimpleRestResponse<EntandoBundle>> response = controller.deployBundle("jwt", bundleInfo);

        // then the expected EntandoBundle is returned
        SimpleRestResponseAssertionHelper.assertOnSuccessfulResponse(response, HttpStatus.OK);
        assertThat(response.getBody().getPayload()).isEqualToComparingFieldByField(expected);
    }

    @Test
    void shouldSuccessfullyUndeployAnExistingEntandoDeBundle() {

        // given an existing bundle in the cluster
        when(bundleService.undeployDeBundle(BundleInfoStubHelper.NAME)).thenReturn(BundleInfoStubHelper.NAME);

        // when the user requests for the undeploy of the bundle
        final ResponseEntity<SimpleRestResponse<DeletedObjectResponse>> response = controller.undeployBundle(
                "jwt", BundleInfoStubHelper.NAME);

        // then the expected successful DeletedObjectResponse is returned
        SimpleRestResponseAssertionHelper.assertOnSuccessfulResponse(response, HttpStatus.OK);
        assertThat(response.getBody().getPayload().getName()).isEqualTo(BundleInfoStubHelper.NAME);
    }


    @Test
    void shouldReturnEmptyArrayWhenReceivingEmptyOrNullParamList() {

        ResponseEntity<SimpleRestResponse<BundlesStatusResult>> response = controller.getBundlesStatusByRepoUrl(null);
        SimpleRestResponseAssertionHelper.assertOnSuccessfulResponse(response, HttpStatus.OK);

        BundlesStatusQuery bundlesStatusQuery = new BundlesStatusQuery();
        response = controller.getBundlesStatusByRepoUrl(bundlesStatusQuery);
        SimpleRestResponseAssertionHelper.assertOnSuccessfulResponse(response, HttpStatus.OK);
    }

    @Test
    void shouldReturnInvalidRepoUrlStatusForInvalidUrl() {

        // given that the bundle service return an empty BundlesStatusResult
        when(bundleService.getBundlesStatus(any())).thenReturn(new BundlesStatusResult());

        // when that the user requests for an invalid url
        BundlesStatusQuery bundlesStatusQuery = new BundlesStatusQuery().setIds(
                List.of(BundleStatusItemStubHelper.ID_INVALID_REPO_URL));
        final ResponseEntity<SimpleRestResponse<BundlesStatusResult>> response = controller.getBundlesStatusByRepoUrl(
                bundlesStatusQuery);

        // then the successful response contains only an invalid url bundle status item
        SimpleRestResponseAssertionHelper.assertOnSuccessfulResponse(response, HttpStatus.OK);
        assertThat(response.getBody().getPayload().getBundlesStatuses()).containsExactlyElementsOf(
                List.of(BundleStatusItemStubHelper.stubBundleStatusItemInvalidRepoUrl()));
    }

    @Test
    void shouldReturnTheExpectedBundleStatusResult() {

        // given that the bundle service return a BundlesStatusResult with 2 elements
        final BundlesStatusResult bundlesStatusResult = new BundlesStatusResult().setBundlesStatuses(List.of(
                BundleStatusItemStubHelper.stubBundleStatusItemInstalled(),
                BundleStatusItemStubHelper.stubBundleStatusItemInstalledNotDeployed()));
        when(bundleService.getBundlesStatus(any())).thenReturn(bundlesStatusResult);

        // when that the user requests for the relative bundles status
        BundlesStatusQuery bundlesStatusQuery = new BundlesStatusQuery().setIds(
                List.of(BundleStatusItemStubHelper.ID_INSTALLED, BundleStatusItemStubHelper.ID_INSTALLED_NOT_DEPLOYED));
        final ResponseEntity<SimpleRestResponse<BundlesStatusResult>> response = controller.getBundlesStatusByRepoUrl(
                bundlesStatusQuery);

        // then the successful response contains the expected bundle status items
        SimpleRestResponseAssertionHelper.assertOnSuccessfulResponse(response, HttpStatus.OK);
        assertThat(response.getBody().getPayload()).isEqualToComparingFieldByField(bundlesStatusResult);
    }

    @Test
    void shouldReturnTheExpectedBundleStatusResultMixingValidAndInvalidUrls() {

        // given that the bundle service return a BundlesStatusResult with 2 elements
        final BundlesStatusResult bundlesStatusResult = new BundlesStatusResult().setBundlesStatuses(
                new ArrayList<>(Arrays.asList(
                        BundleStatusItemStubHelper.stubBundleStatusItemInstalled(),
                        BundleStatusItemStubHelper.stubBundleStatusItemInstalledNotDeployed())));
        when(bundleService.getBundlesStatus(any())).thenReturn(bundlesStatusResult);

        // when that the user requests for the relative bundles status and for an invalid url
        BundlesStatusQuery bundlesStatusQuery = new BundlesStatusQuery().setIds(
                List.of(BundleStatusItemStubHelper.ID_INVALID_REPO_URL,
                        BundleStatusItemStubHelper.ID_INSTALLED, BundleStatusItemStubHelper.ID_INSTALLED_NOT_DEPLOYED));
        final ResponseEntity<SimpleRestResponse<BundlesStatusResult>> response = controller.getBundlesStatusByRepoUrl(
                bundlesStatusQuery);

        // then the successful response contains the expected bundle status items and the invalid url one
        SimpleRestResponseAssertionHelper.assertOnSuccessfulResponse(response, HttpStatus.OK);
        BundlesStatusResult expected = new BundlesStatusResult().setBundlesStatuses(List.of(
                BundleStatusItemStubHelper.stubBundleStatusItemInstalled(),
                BundleStatusItemStubHelper.stubBundleStatusItemInstalledNotDeployed(),
                BundleStatusItemStubHelper.stubBundleStatusItemInvalidRepoUrl()));
        assertThat(response.getBody().getPayload()).isEqualToComparingFieldByField(expected);
    }

    @Test
    void getSingleBundleStatusByNameShouldReturnTheExpectedBundle() {

        // given that the bundle service return a BundlesStatusItem
        final BundlesStatusItem bundlesStatusItem = BundleStatusItemStubHelper.stubBundleStatusItemInstalled();
        when(bundleService.getSingleBundleStatus(bundlesStatusItem.getName())).thenReturn(bundlesStatusItem);

        // when that the user requests for the relative bundle status
        final ResponseEntity<SimpleRestResponse<BundlesStatusItem>> response = controller.getSingleBundleStatusByName(
                BundleStatusItemStubHelper.NAME_INSTALLED);

        // then the successful response contains the expected bundle status item
        SimpleRestResponseAssertionHelper.assertOnSuccessfulResponse(response, HttpStatus.OK);
        assertThat(response.getBody().getPayload()).isEqualToComparingFieldByField(
                BundleStatusItemStubHelper.stubBundleStatusItemInstalled());
    }

    @Test
    void getBundleByRestNamedId_withRepoUrlNamedParam_shouldReturnABundle() {

        String base64EncodedRepoUrl = "aHR0cHM6Ly9naXRodWIuY29tL2ZpcmVnbG92ZXMtYnVuZGxlcy94bWFzYnVuZGxlLmdpdCc";
        final RestNamedId id = RestNamedId.of("repoUrl",
                base64EncodedRepoUrl);
        final EntandoBundle bundle = TestEntitiesGenerator.getTestEntandoBundle();
        when(
                bundleService.getBundleByRepoUrl(base64EncodedRepoUrl)).thenReturn(
                Optional.ofNullable(bundle));
        final ResponseEntity<SimpleRestResponse<EntandoBundle>> response = controller.getBundleByRestNamedId(id);
        assertThat(response.getBody().getPayload()).isEqualToComparingFieldByField(bundle);
    }

    @Test
    void getBundleByRestNamedId_withInvalidResourceId_shouldThrowException() {
        final RestNamedId id = RestNamedId.from("abc");
        assertThrows(DefaultProblem.class, () -> controller.getBundleByRestNamedId(id));
    }
}
