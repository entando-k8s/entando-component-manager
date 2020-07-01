package org.entando.kubernetes.controller.digitalexchange.job;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.exception.job.JobNotFoundException;
import org.entando.kubernetes.exception.k8ssvc.BundleNotFoundException;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleInstallService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleUninstallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class EntandoBundleOperationResourceController implements EntandoBundleOperationResource {

    private final @NonNull EntandoBundleInstallService installService;
    private final @NonNull EntandoBundleUninstallService uninstallService;

    @Override
    public ResponseEntity<SimpleRestResponse<EntandoBundleJob>> install(
            @PathVariable("component") String componentId,
            @RequestParam(name = "version", required = true, defaultValue = "latest") String version) {

        EntandoBundleJob installJob;
        try {
            installJob = installService.install(componentId, version);
        } catch (K8SServiceClientException ex) {
            throw new BundleNotFoundException(componentId);
        }
        return ResponseEntity.created(
                getJobLocationURI(installJob))
                .body(new SimpleRestResponse<>(installJob));
    }


    @Override
    public SimpleRestResponse<EntandoBundleJob> getLastInstallJob(@PathVariable("component") String componentId) {
        EntandoBundleJob lastInstallJob = installService.getAllJobs(componentId)
                .stream().filter(j -> j.getStatus().isOfType(JobType.INSTALL))
                .findFirst()
                .orElseThrow(JobNotFoundException::new);

        return new SimpleRestResponse<>(lastInstallJob);
    }

    @Override
    public ResponseEntity<SimpleRestResponse<EntandoBundleJob>> uninstall(
            @PathVariable("component") String componentId, HttpServletRequest request) {
        EntandoBundleJob uninstallJob = uninstallService.uninstall(componentId);
        return ResponseEntity.created(
                getJobLocationURI(uninstallJob))
                .body(new SimpleRestResponse<>(uninstallJob));
    }

    @Override
    public SimpleRestResponse<EntandoBundleJob> getLastUninstallJob(@PathVariable("component") String componentId) {
        EntandoBundleJob lastUninstallJob = installService.getAllJobs(componentId)
                .stream()
                .filter(j -> j.getStatus().isOfType(JobType.UNINSTALL))
                .findFirst()
                .orElseThrow(JobNotFoundException::new);
        return new SimpleRestResponse<>(lastUninstallJob);
    }

    private URI getJobLocationURI(EntandoBundleJob job) {
        return MvcUriComponentsBuilder
                .fromMethodCall(on(EntandoBundleJobResourceController.class).getJob(job.getId().toString()))
                .build().toUri();
    }

}
