package com.hartwig.platinum.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import com.hartwig.platinum.config.BatchConfiguration;
import com.hartwig.platinum.config.ImmutableBatchConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.config.ServiceAccountConfiguration;
import com.hartwig.platinum.kubernetes.JobSubmitter;
import com.hartwig.platinum.kubernetes.KubernetesClientProxy;

import org.junit.Test;

public class JobSchedulerTest {
    private final JobSubmitter jobSubmitter = mock(JobSubmitter.class);
    private final KubernetesClientProxy kubernetesClientProxy = mock(KubernetesClientProxy.class);
    private static final ServiceAccountConfiguration SA = ServiceAccountConfiguration.builder().kubernetesServiceAccount("ksa").gcpEmailAddress("email").build();

    @Test
    public void shouldConstructTimedBatchSchedulerInstanceIfDelaySpecified() {
        assertThat(createFromConfiguration(20)).isInstanceOf(TimedBatchScheduler.class);
    }

    @Test
    public void shouldConstructConstantJobCountSchedulerInstanceIfBatchSizeSpecifiedWithoutDelay() {
        assertThat(createFromConfiguration(null)).isInstanceOf(ConstantJobCountScheduler.class);
    }

    @Test
    public void shouldConstructConstantJobCountSchedulerOfSizeOneIfBatchConfigurationUnspecified() {
        PlatinumConfiguration configuration = PlatinumConfiguration.builder()
                .serviceAccount(SA)
                .build();
        assertThat(JobScheduler.fromConfiguration(configuration, jobSubmitter, kubernetesClientProxy))
                .isInstanceOf(ConstantJobCountScheduler.class);
    }

    private JobScheduler createFromConfiguration(Integer delay) {
        ImmutableBatchConfiguration.Builder batchConfigurationBuilder = ImmutableBatchConfiguration.builder().size(10);
        Optional.ofNullable(delay).ifPresent(batchConfigurationBuilder::delay);
        BatchConfiguration batchConfiguration = batchConfigurationBuilder.build();
        PlatinumConfiguration configuration = PlatinumConfiguration.builder().batch(batchConfiguration).serviceAccount(SA).build();
        return JobScheduler.fromConfiguration(configuration, jobSubmitter, kubernetesClientProxy);
    }
}