package com.hartwig.platinum.kubernetes.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import com.hartwig.platinum.config.BatchConfiguration;
import com.hartwig.platinum.config.ImmutableBatchConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.kubernetes.JobSubmitter;

import org.junit.Test;

public class JobSchedulerTest {
    private JobSubmitter jobSubmitter;

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
        jobSubmitter = mock(JobSubmitter.class);
        PlatinumConfiguration configuration = PlatinumConfiguration.builder().build();
        assertThat(JobScheduler.fromConfiguration(configuration, jobSubmitter)).isInstanceOf(ConstantJobCountScheduler.class);
    }

    private JobScheduler createFromConfiguration(Integer delay) {
        jobSubmitter = mock(JobSubmitter.class);
        ImmutableBatchConfiguration.Builder batchConfigurationBuilder = ImmutableBatchConfiguration.builder().size(10);
        Optional.ofNullable(delay).ifPresent(batchConfigurationBuilder::delay);
        BatchConfiguration batchConfiguration = batchConfigurationBuilder.build();
        PlatinumConfiguration configuration = PlatinumConfiguration.builder().batch(batchConfiguration).build();
        return JobScheduler.fromConfiguration(configuration, jobSubmitter);
    }
}