package org.entando.kubernetes.service.digitalexchange.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.entando.kubernetes.client.EntandoBundleComponentJobRepositoryTestDouble;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDelete;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDeleteResponseStatus;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDeleteStatus;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.JobResult;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("unit")
class BundleUninstallUtilityTest {

    private EntandoBundleComponentJobRepository compJobRepo;
    private BundleUninstallUtility bundleUninstallUtility;

    @BeforeEach
    void init() {
        compJobRepo = Mockito.spy(EntandoBundleComponentJobRepositoryTestDouble.class);
        bundleUninstallUtility = new BundleUninstallUtility(compJobRepo, new ObjectMapper());
    }


    @Test
    void shouldSetErrorsIfResponseIsFailureOrPartialSuccess() {
        EntandoCoreComponentDeleteResponse response =
                EntandoCoreComponentDeleteResponse.builder().status(
                        EntandoCoreComponentDeleteResponseStatus.FAILURE).components(Arrays.asList(
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "A",
                                EntandoCoreComponentDeleteStatus.FAILURE),
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "B",
                                EntandoCoreComponentDeleteStatus.FAILURE))).build();
        JobResult jobResult = new JobResult();
        jobResult.setStatus(JobStatus.UNINSTALL_IN_PROGRESS);
        bundleUninstallUtility.markGlobalError(jobResult, response);


        EntandoBundleComponentJobEntity cjeA = new EntandoBundleComponentJobEntity();
        cjeA.setComponentType(ComponentType.CONTENT_TYPE);
        cjeA.setComponentId("A");
        EntandoBundleComponentJobEntity cjeB = new EntandoBundleComponentJobEntity();
        cjeB.setComponentType(ComponentType.CONTENT_TYPE);
        cjeB.setComponentId("B");

        List<EntandoBundleComponentJobEntity> toDelete = Arrays.asList(cjeA, cjeB);
        bundleUninstallUtility.markSingleErrors(toDelete, response);

        // we expect a global error mark because the uninstallation was not successful
        assertNotNull(jobResult.getUninstallException());
        List<EntandoBundleComponentJobEntity> componentJobEntities = compJobRepo.findAll();
        // the "markSingleErrors" method should update only those entities whose response status is FAILURE. So we expect 2
        assertEquals(2, componentJobEntities.size());
        // we expect both the error message and the code to be compiled for the uninstallation process.
        componentJobEntities.forEach(componentJobEntity -> {
            assertFalse(componentJobEntity.getUninstallErrorMessage().isEmpty());
            assertEquals(100, componentJobEntity.getUninstallErrorCode());
        });
    }

    @Test
    void shouldSetErrorsOnComponentInFailureStateIfDeleteResponseIsPartialSuccess() {
        // --GIVEN
        EntandoCoreComponentDeleteResponse response =
                EntandoCoreComponentDeleteResponse.builder().status(
                        EntandoCoreComponentDeleteResponseStatus.PARTIAL_SUCCESS).components(Arrays.asList(
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "A",
                                EntandoCoreComponentDeleteStatus.SUCCESS),
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "B",
                                EntandoCoreComponentDeleteStatus.FAILURE))).build();

        JobResult jobResult = new JobResult();
        jobResult.setStatus(JobStatus.UNINSTALL_IN_PROGRESS);
        bundleUninstallUtility.markGlobalError(jobResult, response);

        EntandoBundleComponentJobEntity cjeA = new EntandoBundleComponentJobEntity();
        cjeA.setComponentType(ComponentType.CONTENT_TYPE);
        cjeA.setComponentId("A");
        EntandoBundleComponentJobEntity cjeB = new EntandoBundleComponentJobEntity();
        cjeB.setComponentType(ComponentType.CONTENT_TYPE);
        cjeB.setComponentId("B");

        List<EntandoBundleComponentJobEntity> toDelete = Arrays.asList(cjeA, cjeB);
        bundleUninstallUtility.markSingleErrors(toDelete, response);
        // --THEN
        // we expect a global error mark because the uninstallation was not successful
        assertNotNull(jobResult.getUninstallException());
        List<EntandoBundleComponentJobEntity> componentJobEntities = compJobRepo.findAll();
        // the "markSingleErrors" method should update only those entities whose response status is FAILURE. So we expect 1
        assertEquals(1, componentJobEntities.size());
        EntandoBundleComponentJobEntity componentB = componentJobEntities.get(0);
        // the only entity should be the one with component id "B"
        assertEquals("B", componentB.getComponentId());
        // we expect both the error message and the code to be compiled for the uninstallation process.
        assertFalse(componentB.getUninstallErrorMessage().isEmpty());
        assertEquals(100, componentB.getUninstallErrorCode());
    }

    @Test
    void shouldThrowExceptionIfResponseComponentsInFailureStateDoesNotMatchAnyComponentsToDelete() {
        EntandoBundleComponentJobEntity cjeA = new EntandoBundleComponentJobEntity();
        cjeA.setComponentType(ComponentType.CONTENT_TYPE);
        cjeA.setComponentId("A");
        EntandoBundleComponentJobEntity cjeB = new EntandoBundleComponentJobEntity();
        cjeB.setComponentType(ComponentType.CONTENT_TYPE);
        cjeB.setComponentId("B");
        EntandoCoreComponentDeleteResponse response =
                EntandoCoreComponentDeleteResponse.builder().status(
                        EntandoCoreComponentDeleteResponseStatus.PARTIAL_SUCCESS).components(Arrays.asList(
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "A",
                                EntandoCoreComponentDeleteStatus.SUCCESS),
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "C",
                                EntandoCoreComponentDeleteStatus.FAILURE))).build();
        List<EntandoBundleComponentJobEntity> toDelete = Arrays.asList(cjeA, cjeB);
        assertThrows(IllegalStateException.class, () -> bundleUninstallUtility.markSingleErrors(toDelete, response));
    }

    @Test
    void shouldThrowExceptionIfResponseComponentsInFailureStateAreMalformed() {
        EntandoBundleComponentJobEntity cjeA = new EntandoBundleComponentJobEntity();
        cjeA.setComponentType(ComponentType.CONTENT_TYPE);
        cjeA.setComponentId("A");
        EntandoBundleComponentJobEntity cjeB = new EntandoBundleComponentJobEntity();
        cjeB.setComponentType(ComponentType.CONTENT_TYPE);
        cjeB.setComponentId("B");
        EntandoCoreComponentDeleteResponse response =
                EntandoCoreComponentDeleteResponse.builder().status(
                        EntandoCoreComponentDeleteResponseStatus.PARTIAL_SUCCESS).components(Arrays.asList(
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "A",
                                EntandoCoreComponentDeleteStatus.SUCCESS),
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, null,
                                EntandoCoreComponentDeleteStatus.FAILURE))).build();

        List<EntandoBundleComponentJobEntity> toDelete = Arrays.asList(cjeA, cjeB);
        assertThrows(IllegalArgumentException.class, () -> bundleUninstallUtility.markSingleErrors(toDelete, response));
    }

    @Test
    void shouldThrowExceptionIfEntitiesToDeleteAreMalformed() {

        // malformed entity (missing component id and type)
        EntandoBundleComponentJobEntity cjeA = new EntandoBundleComponentJobEntity();
        EntandoBundleComponentJobEntity cjeB = new EntandoBundleComponentJobEntity();
        cjeB.setComponentType(ComponentType.CONTENT_TYPE);
        cjeB.setComponentId("B");

        EntandoCoreComponentDeleteResponse response =
                EntandoCoreComponentDeleteResponse.builder().status(
                        EntandoCoreComponentDeleteResponseStatus.PARTIAL_SUCCESS).components(List.of()).build();

        List<EntandoBundleComponentJobEntity> toDelete = Arrays.asList(cjeA, cjeB);
        assertThrows(IllegalArgumentException.class, () -> bundleUninstallUtility.markSingleErrors(toDelete, response));
    }
}
