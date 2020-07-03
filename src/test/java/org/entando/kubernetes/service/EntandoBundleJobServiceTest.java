package org.entando.kubernetes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.FilterOperator;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleJobListProcessor;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("in-process")
public class EntandoBundleJobServiceTest {

    private EntandoBundleJobRepository jobRepository;
    private EntandoBundleComponentJobRepository componentJobRepository;
    private EntandoBundleJobService jobService;

    @BeforeEach
    public void setup() {
        this.jobRepository = Mockito.mock(EntandoBundleJobRepository.class);
        this.componentJobRepository = Mockito.mock(EntandoBundleComponentJobRepository.class);
        jobService = new EntandoBundleJobService(jobRepository, componentJobRepository);
    }

    @Test
    public void shouldReturnAllJobs() {
        EntandoBundleJob testJob1 = new EntandoBundleJob();
        testJob1.setId(UUID.randomUUID());
        testJob1.setComponentId("component-1-id");
        testJob1.setComponentName("component-1-name");
        testJob1.setComponentVersion("component-1-version");
        testJob1.setStatus(JobStatus.INSTALL_COMPLETED);
        testJob1.setProgress(1.0);
        testJob1.setStartedAt(LocalDateTime.of(2020, 4, 22, 8, 10, 0));
        testJob1.setFinishedAt(LocalDateTime.of(2020, 4, 22, 8, 15, 0));

        EntandoBundleJob testJob2 = new EntandoBundleJob();
        testJob2.setId(UUID.randomUUID());
        testJob2.setComponentId("component-2-id");
        testJob2.setComponentName("component-2-name");
        testJob2.setComponentVersion("component-2-version");
        testJob2.setStatus(JobStatus.INSTALL_ROLLBACK);
        testJob2.setProgress(1.0);
        testJob2.setStartedAt(LocalDateTime.of(2020, 4, 22, 20, 0, 0));
        testJob2.setFinishedAt(LocalDateTime.of(2020, 4, 22, 21, 15, 0));

        EntandoBundleJob testJob3 = new EntandoBundleJob();
        testJob3.setId(UUID.randomUUID());
        testJob3.setComponentId("component-1-id");
        testJob3.setComponentName("component-1-name");
        testJob3.setComponentVersion("component-1-version");
        testJob3.setStatus(JobStatus.UNINSTALL_IN_PROGRESS);
        testJob3.setProgress(0.5);
        testJob3.setStartedAt(LocalDateTime.of(2020, 4, 23, 15, 1, 30));

        when(jobService.getJobs()).thenReturn(Arrays.asList(testJob1, testJob2, testJob3));


        List<EntandoBundleJob> allJobs = jobService.getJobs();
        assertThat(allJobs.size()).isEqualTo(3);
        assertThat(allJobs.get(0).getStartedAt()).isBefore(allJobs.get(1).getStartedAt());
        assertThat(allJobs.get(0).getId()).isEqualByComparingTo(testJob1.getId());
        assertThat(allJobs.get(2).getId()).isEqualByComparingTo(testJob3.getId());
    }

    @Test
    public void shouldFilterAndSort() {
        EntandoBundleJob testJob1 = new EntandoBundleJob();
        testJob1.setId(UUID.randomUUID());
        testJob1.setComponentId("component-1-id");
        testJob1.setComponentName("component-1-name");
        testJob1.setComponentVersion("component-1-version");
        testJob1.setStatus(JobStatus.INSTALL_COMPLETED);
        testJob1.setProgress(1.0);
        testJob1.setStartedAt(LocalDateTime.of(2020, 4, 22, 8, 10, 0));
        testJob1.setFinishedAt(LocalDateTime.of(2020, 4, 22, 8, 15, 0));

        EntandoBundleJob testJob2 = new EntandoBundleJob();
        testJob2.setId(UUID.randomUUID());
        testJob2.setComponentId("component-2-id");
        testJob2.setComponentName("component-2-name");
        testJob2.setComponentVersion("component-2-version");
        testJob2.setStatus(JobStatus.INSTALL_ROLLBACK);
        testJob2.setProgress(1.0);
        testJob2.setStartedAt(LocalDateTime.of(2020, 4, 22, 20, 0, 0));
        testJob2.setFinishedAt(LocalDateTime.of(2020, 4, 22, 21, 15, 0));

        EntandoBundleJob testJob3 = new EntandoBundleJob();
        testJob3.setId(UUID.randomUUID());
        testJob3.setComponentId("component-1-id");
        testJob3.setComponentName("component-1-name");
        testJob3.setComponentVersion("component-1-version");
        testJob3.setStatus(JobStatus.UNINSTALL_IN_PROGRESS);
        testJob3.setProgress(0.5);
        testJob3.setStartedAt(LocalDateTime.of(2020, 4, 23, 15, 1, 30));

        when(jobRepository.findAllByOrderByStartedAtDesc()).thenReturn(Arrays.asList(testJob1, testJob2, testJob3));

        PagedListRequest request = new PagedListRequest();
        request.addFilter(new Filter(EntandoBundleJobListProcessor.ID, testJob1.getId().toString()));
        PagedMetadata<EntandoBundleJob> jobs = jobService.getJobs(request);

        assertThat(jobs.getTotalItems()).isEqualTo(1);
        assertThat(jobs.getBody()).hasSize(1);
        assertThat(jobs.getBody().get(0)).isEqualTo(testJob1);

        request = new PagedListRequest();
        request.addFilter(new Filter(EntandoBundleJobListProcessor.COMPONENT_NAME, "component-1-name"));
        request.setSort(EntandoBundleJobListProcessor.STARTED_AT);
        request.setDirection(Filter.ASC_ORDER);

        jobs = jobService.getJobs(request);
        assertThat(jobs.getBody()).hasSize(2);
        assertThat(jobs.getBody().get(0)).isEqualTo(testJob1);
        assertThat(jobs.getBody().get(1)).isEqualTo(testJob3);


        request = new PagedListRequest();
        Filter filterValue = new Filter();
        filterValue.setAttribute(EntandoBundleJobListProcessor.STATUS);
        filterValue.setOperator(FilterOperator.EQUAL.getValue());
        filterValue.setAllowedValues(new String[]{
                JobStatus.INSTALL_COMPLETED.name(),
                JobStatus.INSTALL_ROLLBACK.name()});
        request.addFilter(filterValue);
        request.setSort(EntandoBundleJobListProcessor.COMPONENT_VERSION);
        request.setDirection(Filter.DESC_ORDER);

        jobs = jobService.getJobs(request);
        assertThat(jobs.getBody()).hasSize(2);
        assertThat(jobs.getBody().get(0)).isEqualTo(testJob2);
        assertThat(jobs.getBody().get(1)).isEqualTo(testJob1);
    }


}
