package com.hartwig.platinum.kubernetes;

import static com.hartwig.platinum.kubernetes.KubernetesCluster.NAMESPACE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Collectors;

import com.hartwig.platinum.kubernetes.pipeline.PipelineJob;

import org.junit.Before;
import org.junit.Test;

import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobCondition;
import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.api.model.batch.JobStatus;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.ScalableResource;

public class JobSubmitterTest {
    private static final String JOB_NAME = "job1";
    @SuppressWarnings("rawtypes")
    private final NonNamespaceOperation jobs = mock(NonNamespaceOperation.class);
    @SuppressWarnings("unchecked")
    private final ScalableResource<Job> scalableJob = mock(ScalableResource.class);
    private final PipelineJob pipelineJob = mock(PipelineJob.class);
    private final JobSpec jobSpec = mock(JobSpec.class);
    private JobSubmitter victim;
    private KubernetesClientProxy kubernetesClientProxy;
    private Job job;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        kubernetesClientProxy = mock(KubernetesClientProxy.class);
        victim = new JobSubmitter(kubernetesClientProxy, false);

        when(pipelineJob.getName()).thenReturn(JOB_NAME);
        when(pipelineJob.asKubernetes()).thenReturn(jobSpec);
        when(kubernetesClientProxy.jobs()).thenReturn(jobs);
        when(jobs.withName(JOB_NAME)).thenReturn(scalableJob);

        job = new JobBuilder().withNewMetadata().withName(JOB_NAME).withNamespace(NAMESPACE).endMetadata().withSpec(jobSpec).build();
    }

    @Test
    public void shouldSubmitNewJobIfNamedJobDoesNotExist() {
        verifyWhetherJobCreatedAndReturnValueIs(true, true);
    }

    @Test
    public void shouldNotSubmitAnythingIfJobCompletedAlready() {
        setupJobConditions(Map.of("Complete", "True", "OtherState", "True"));
        verifyWhetherJobCreatedAndReturnValueIs(false, false);
    }

    @Test
    public void shouldNotSubmitAnythingIfJobFailedAndRetryIsFalse() {
        setupJobConditions(Map.of("Failed", "True", "Complete", "False"));
        verifyWhetherJobCreatedAndReturnValueIs(false, false);
    }

    @Test
    public void shouldResubmitFailedJobIfRetryIsTrue() {
        victim = new JobSubmitter(kubernetesClientProxy, true);
        setupJobConditions(Map.of("Failed", "True", "Running", "False"));
        verifyWhetherJobCreatedAndReturnValueIs(true, true);
    }

    @Test
    public void shouldNotResubmitRunningJobButShouldTellSchedulerItDidSubmitIt() {
        setupJobConditions(Map.of("Complete", "False"));
        verifyWhetherJobCreatedAndReturnValueIs(false, true);
    }

    private void setupJobConditions(Map<String, String> statuses) {
        Job existingJob = mock(Job.class);
        JobStatus jobStatus = mock(JobStatus.class);
        when(existingJob.getStatus()).thenReturn(jobStatus);
        when(jobStatus.getConditions()).thenReturn(statuses.keySet()
                .stream()
                .map(status -> new JobCondition(null, null, null, null, statuses.get(status), status))
                .collect(Collectors.toList()));
        when(scalableJob.get()).thenReturn(existingJob);
    }

    @SuppressWarnings("unchecked")
    private void verifyWhetherJobCreatedAndReturnValueIs(boolean jobCreated, boolean returnValue) {
        assertThat(victim.submit(pipelineJob)).isEqualTo(returnValue);
        if (jobCreated) {
            verify(jobs).create(job);
        } else {
            verify(jobs, never()).create(any());
        }
    }
}