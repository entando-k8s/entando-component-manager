package org.entando.kubernetes.controller.digitalexchange.job;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlansRequest;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallWithPlansRequest;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.exception.job.JobNotFoundException;
import org.entando.kubernetes.exception.k8ssvc.BundleNotFoundException;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.model.web.response.RestError;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.security.AuthorizationChecker;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleInstallService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleJobService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleUninstallService;
import org.entando.kubernetes.validator.InstallPlanValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class EntandoBundleOperationResourceController implements EntandoBundleOperationResource {

    private final @NonNull KubernetesService kubeService;
    private final @NonNull EntandoBundleJobService jobService;
    private final @NonNull EntandoBundleInstallService installService;
    private final @NonNull EntandoBundleUninstallService uninstallService;
    private final @NonNull InstallPlanValidator installPlanValidator;
    private final @NonNull AuthorizationChecker authorizationChecker;
    private final @NonNull EntandoBundleService bundleService;

    @Override
    public ResponseEntity<SimpleRestResponse<InstallPlan>> installPlans(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("component") String componentId,
            @RequestBody(required = false) InstallPlansRequest installPlansRequest) {

        this.authorizationChecker.checkPermissions(authorizationHeader);

        final InstallPlansRequest request = Optional.ofNullable(installPlansRequest).orElse(new InstallPlansRequest());

        EntandoDeBundle bundle = kubeService.fetchBundleByName(componentId)
                .orElseThrow(() -> new BundleNotFoundException(componentId));
        EntandoDeBundleTag tag = getBundleTagOrFail(bundle, request.getVersion());

        InstallPlan installPlan = installService
                .generateInstallPlan(bundle, tag, EntandoBundleInstallService.PERFORM_CONCURRENT_CHECKS);

        return ResponseEntity.ok(new SimpleRestResponse<>(installPlan));
    }


    @Override
    public ResponseEntity<SimpleRestResponse<EntandoBundleJobEntity>> install(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("component") String componentId,
            @RequestBody(required = false) InstallRequest installRequest) {

        this.authorizationChecker.checkPermissions(authorizationHeader);

        final InstallRequest request = Optional.ofNullable(installRequest).orElse(new InstallRequest());

        if (checkConflictOnBundleAndRequest(componentId, request)) {
            String errorMessage = String.format(
                    "Error install request conflict strategy:'%s' not compatible with the status of the bundle:'%s'",
                    request.getConflictStrategy(), componentId);
            throw new JobConflictException(errorMessage);
        } else {

            EntandoDeBundle bundle = kubeService.fetchBundleByName(componentId)
                    .orElseThrow(() -> new BundleNotFoundException(componentId));
            EntandoDeBundleTag tag = getBundleTagOrFail(bundle, request.getVersion());

            EntandoBundleJobEntity installJob = jobService.findCompletedOrConflictingInstallJob(bundle)
                    .orElseGet(() -> installService.install(bundle, tag, request.getConflictStrategy()));

            return ResponseEntity.created(
                            getJobLocationURI(installJob))
                    .body(new SimpleRestResponse<>(installJob));
        }
    }

    private boolean checkConflictOnBundleAndRequest(String componentId, InstallRequest installRequest) {
        if (bundleService.getInstalledBundle(componentId).map(b -> b.isInstalled()).orElse(Boolean.FALSE)
                && strategyCretateOrNull(installRequest)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean strategyCretateOrNull(InstallRequest installRequest) {
        return installRequest.getConflictStrategy() == null
                || InstallAction.CREATE.equals(installRequest.getConflictStrategy());
    }

    @Override
    public ResponseEntity<SimpleRestResponse<EntandoBundleJobEntity>> installWithInstallPlan(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("component") String componentId,
            @RequestBody(required = false) InstallWithPlansRequest installRequest) {

        this.authorizationChecker.checkPermissions(authorizationHeader);

        final InstallWithPlansRequest request = Optional.ofNullable(installRequest)
                .orElse(new InstallWithPlansRequest());

        installPlanValidator.validateInstallPlanOrThrow(installRequest);

        EntandoDeBundle bundle = kubeService.fetchBundleByName(componentId)
                .orElseThrow(() -> new BundleNotFoundException(componentId));
        EntandoDeBundleTag tag = getBundleTagOrFail(bundle, request.getVersion());

        EntandoBundleJobEntity installJob = jobService.findCompletedOrConflictingInstallJob(bundle)
                .orElseGet(() -> installService.installWithInstallPlan(bundle, tag, request));

        return ResponseEntity.created(
                getJobLocationURI(installJob))
                .body(new SimpleRestResponse<>(installJob));
    }

    @Override
    public SimpleRestResponse<EntandoBundleJobEntity> getLastInstallJob(@PathVariable("component") String componentId) {
        return new SimpleRestResponse<>(executeGetLastInstallJow(componentId));
    }

    @Override
    public SimpleRestResponse<EntandoBundleJobEntity> getLastInstallJobWithInstallPlan(String componentId) {
        return new SimpleRestResponse<>(executeGetLastInstallJow(componentId));
    }

    private EntandoBundleJobEntity executeGetLastInstallJow(String componentId) {
        return jobService.getJobs(componentId)
                .stream().filter(j -> j.getStatus().isOfType(JobType.INSTALL))
                .findFirst()
                .orElseThrow(JobNotFoundException::new);
    }

    @Override
    public ResponseEntity<SimpleRestResponse<EntandoBundleJobEntity>> uninstall(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("component") String componentId, HttpServletRequest request) {

        this.authorizationChecker.checkPermissions(authorizationHeader);

        try {
            EntandoBundleJobEntity uninstallJob = uninstallService.uninstall(componentId);
            return ResponseEntity.created(
                    getJobLocationURI(uninstallJob))
                    .body(new SimpleRestResponse<>(uninstallJob));
        } catch (JobConflictException e) {
            SimpleRestResponse<EntandoBundleJobEntity> restResponse = new SimpleRestResponse<>();
            restResponse.setErrors(Collections.singletonList(new RestError("100", e.getMessage())));

            return ResponseEntity.status(e.getStatus())
                    .body(restResponse);
        }
    }

    @Override
    public SimpleRestResponse<EntandoBundleJobEntity> getLastUninstallJob(
            @PathVariable("component") String componentId) {
        EntandoBundleJobEntity lastUninstallJob = jobService.getJobs(componentId)
                .stream()
                .filter(j -> j.getStatus().isOfType(JobType.UNINSTALL))
                .findFirst()
                .orElseThrow(JobNotFoundException::new);
        return new SimpleRestResponse<>(lastUninstallJob);
    }

    private URI getJobLocationURI(EntandoBundleJobEntity job) {
        return MvcUriComponentsBuilder
                .fromMethodCall(on(EntandoBundleJobResourceController.class).getJob(job.getId().toString()))
                .build().toUri();
    }

    private EntandoDeBundleTag getBundleTagOrFail(EntandoDeBundle bundle, String version) {
        String versionToFind = BundleUtilities.getBundleVersionOrFail(bundle, version);
        return bundle.getSpec().getTags().stream().filter(t -> t.getVersion().equals(versionToFind)).findAny()
                .orElseThrow(
                        () -> new InvalidBundleException("Version " + versionToFind + " not defined in bundle versions"));
    }

}
