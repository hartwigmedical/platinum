package com.hartwig.platinum.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import com.hartwig.platinum.kubernetes.JobSubmitter;
import com.hartwig.platinum.kubernetes.KubernetesClientProxy;
import com.hartwig.platinum.kubernetes.pipeline.PipelineJob;

import org.junit.Before;
import org.junit.Test;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.ScalableResource;

public class ConstantJobCountSchedulerTest {
    private final JobSubmitter jobSubmitter = mock(JobSubmitter.class);
    private final KubernetesClientProxy kubernetesClientProxy = mock(KubernetesClientProxy.class);
    private final Delay submissionDelay = mock(Delay.class);
    private final PipelineJob job1 = mock(PipelineJob.class);
    private final PipelineJob job2 = mock(PipelineJob.class);
    private final PipelineJob job3 = mock(PipelineJob.class);
    @SuppressWarnings("unchecked")
    private final ScalableResource<Job> scalableJob1 = mock(ScalableResource.class);
    @SuppressWarnings("unchecked")
    private final ScalableResource<Job> scalableJob2 = mock(ScalableResource.class);
    @SuppressWarnings("unchecked")
    private final ScalableResource<Job> scalableJob3 = mock(ScalableResource.class);
    @SuppressWarnings("rawtypes")
    private final NonNamespaceOperation jobs = mock(NonNamespaceOperation.class);
    private final Job kubeJob1 = mock(Job.class);
    private final Job kubeJob2 = mock(Job.class);
    private final Job kubeJob3 = mock(Job.class);
    private JobStatus kubeJob1Status;
    private static final String CONDITION_COMPLETE = "Complete";
    private static final String CONDITION_FAILED = "Failed";
    private JobStatus kubeJob2Status;
    private JobStatus kubeJob3Status;

    private static enum Condition {
        COMPLETE("Complete"),
        FAILED("Failed");

        private final String type;

        Condition(String type) {
            this.type = type;
        }
    }

    private ConstantJobCountScheduler victim;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        when(job1.getName()).thenReturn("job1");
        when(job2.getName()).thenReturn("job2");
        when(job3.getName()).thenReturn("job3");

        when(jobSubmitter.submit(any(PipelineJob.class))).thenReturn(true);
        when(kubernetesClientProxy.jobs()).thenReturn(jobs);

        when(jobs.withName("job1")).thenReturn(scalableJob1);
        when(jobs.withName("job2")).thenReturn(scalableJob2);
        when(jobs.withName("job3")).thenReturn(scalableJob3);

        when(scalableJob1.get()).thenReturn(kubeJob1);
        when(scalableJob2.get()).thenReturn(kubeJob2);
        when(scalableJob3.get()).thenReturn(kubeJob3);

        kubeJob1Status = mock(JobStatus.class);
        kubeJob2Status = mock(JobStatus.class);
        kubeJob3Status = mock(JobStatus.class);

        when(kubeJob1.getStatus()).thenReturn(kubeJob1Status);
        when(kubeJob2.getStatus()).thenReturn(kubeJob2Status);
        when(kubeJob3.getStatus()).thenReturn(kubeJob3Status);

        when(kubeJob1Status.getConditions()).thenReturn(Collections.emptyList());
        when(kubeJob2Status.getConditions()).thenReturn(Collections.emptyList());
        when(kubeJob3Status.getConditions()).thenReturn(Collections.emptyList());

        victim = new ConstantJobCountScheduler(jobSubmitter, kubernetesClientProxy, 2, submissionDelay, Delay.forMilliseconds(1));
    }

    @Test
    public void shouldWaitUntilThereIsSpaceToScheduleJob() {
        when(kubeJob1Status.getConditions()).thenReturn(Collections.emptyList()).thenReturn(List.of(mockCondition(CONDITION_COMPLETE)));

        victim.submit(job1);
        victim.submit(job2);
        victim.submit(job3);

        verify(submissionDelay, times(3)).threadSleep();
        verify(jobSubmitter).submit(job3);
    }

    @Test
    public void shouldNotRescheduleJobThatHasDisappeared() {
        when(scalableJob1.get()).thenReturn(kubeJob1).thenReturn(null);
        when(kubeJob1Status.getConditions()).thenReturn(Collections.emptyList());

        victim.submit(job1);
        victim.submit(job2);
        victim.submit(job3);

        verify(jobSubmitter, times(1)).submit(job1);
    }

    @Test
    public void shouldReAuthoriseWhenKubernetesClientExceptionOccurs() {
        when(scalableJob1.get()).thenThrow(KubernetesClientException.class).thenReturn(kubeJob1);
        List<JobCondition> conditions = List.of(mockCondition(CONDITION_COMPLETE));
        when(kubeJob1Status.getConditions()).thenReturn(conditions);

        victim.submit(job1);
        victim.submit(job2);
        victim.submit(job3);

        verify(kubernetesClientProxy).authorise();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldResubmitFailedJob() {
        List<JobCondition> jobConditions = List.of(mockCondition(CONDITION_FAILED));
        when(kubeJob1Status.getConditions()).thenReturn(jobConditions).thenReturn(List.of(mockCondition(CONDITION_COMPLETE)));
        victim.submit(job1);
        victim.submit(job2);
        victim.submit(job3);

        verify(jobs).delete(kubeJob1);
        verify(jobSubmitter, times(2)).submit(job1);
    }

    private static JobCondition mockCondition(String condition) {
        JobCondition jobCondition = mock(JobCondition.class);
        when(jobCondition.getStatus()).thenReturn("True");
        when(jobCondition.getType()).thenReturn(condition);
        return jobCondition;
    }
}