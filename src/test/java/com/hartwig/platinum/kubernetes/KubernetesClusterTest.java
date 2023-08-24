package com.hartwig.platinum.kubernetes;

import com.hartwig.pdl.PipelineInput;
import com.hartwig.pdl.SampleInput;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.ImmutableGcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.kubernetes.pipeline.ImmutableSampleArgument;
import com.hartwig.platinum.kubernetes.pipeline.PipelineConfigMapBuilder;
import com.hartwig.platinum.kubernetes.pipeline.PipelineJob;
import com.hartwig.platinum.kubernetes.pipeline.SampleArgument;
import com.hartwig.platinum.scheduling.JobScheduler;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class KubernetesClusterTest {
    private static final ImmutableGcpConfiguration GCP = GcpConfiguration.builder().project("project").region("region").build();
    private static final String CONFIG = "config";
    private static final List<SampleArgument> SAMPLES = List.of(sample());
    private List<Supplier<PipelineInput>> pipelineInputs;
    private KubernetesCluster victim;
    private JobScheduler scheduler;
    private PipelineConfigMapBuilder configMaps;

    @Before
    public void setUp() {
        scheduler = mock(JobScheduler.class);
        configMaps = mock(PipelineConfigMapBuilder.class);
        SampleInput tumor = SampleInput.builder().name("tumor-a").build();
        PipelineInput input1 = PipelineInput.builder().setName("setName").tumor(tumor).build();
        pipelineInputs = List.of(() -> input1);
        when(configMaps.forSample(any(), any())).thenReturn(new VolumeBuilder().withName(CONFIG).build());
    }

    @Test
    public void addsJksVolumeAndContainerIfPasswordSpecified() {
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victim = victimise(PlatinumConfiguration.builder().keystorePassword("changeit").gcp(GCP).build());
        victim.submit();
        verify(scheduler).submit(job.capture());
        PipelineJob result = job.getValue();
        assertThat(result.getVolumes()).extracting(Volume::getName).contains("jks");
        assertThat(result.getContainer().getEnv()).hasSize(1);
        EnvVar env = result.getContainer().getEnv().get(0);
        assertThat(env.getName()).isEqualTo("JAVA_OPTS");
        assertThat(env.getValue()).isEqualTo("-Djavax.net.ssl.keyStore=/jks/api.jks -Djavax.net.ssl.keyStorePassword=changeit");
    }

    @Test
    public void submitsJobsToTheScheduler() {
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victimise(PlatinumConfiguration.builder().keystorePassword("changeit").gcp(GCP).build()).submit();
        verify(scheduler).submit(job.capture());
        assertThat(job.getAllValues().size()).isEqualTo(SAMPLES.size());
    }

    @Test
    public void addsConfigMapAndSecretVolumes() {
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victim = victimise(PlatinumConfiguration.builder().gcp(GCP).build());
        victim.submit();
        verify(scheduler).submit(job.capture());
        assertThat(job.getValue().getVolumes()).extracting(Volume::getName).containsExactly(CONFIG);
    }

    @Test
    public void addsConfigMapForSampleToEachJob() {
        PipelineInput inputA = PipelineInput.builder().setName("set-a").tumor(SampleInput.builder().name("tumor-a").build()).build();
        PipelineInput inputB = PipelineInput.builder().setName("set-b").tumor(SampleInput.builder().name("tumor-b").build()).build();
        pipelineInputs = List.of(() -> inputA, () -> inputB);

        when(configMaps.forSample("tumor-a", inputA)).thenReturn(new VolumeBuilder().withName("config-a").build());
        when(configMaps.forSample("tumor-b", inputB)).thenReturn(new VolumeBuilder().withName("config-b").build());
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victimise(PlatinumConfiguration.builder().gcp(GCP).build()).submit();
        verify(scheduler, times(2)).submit(job.capture());
        List<PipelineJob> allJobs = job.getAllValues();
        List<PipelineJob> jobsA = allJobs.stream().filter(j -> j.getName().equals("tumor-a-test")).collect(Collectors.toList());
        assertThat(jobsA.size()).isEqualTo(1);
        assertThat(jobsA.get(0).getVolumes().stream().filter(v -> v.getName().equals("config-a")).collect(Collectors.toList())).hasSize(1);
        List<PipelineJob> jobsB = allJobs.stream().filter(j -> j.getName().equals("tumor-b-test")).collect(Collectors.toList());
        assertThat(jobsB.size()).isEqualTo(1);
        assertThat(jobsB.get(0).getVolumes().stream().filter(v -> v.getName().equals("config-b")).collect(Collectors.toList())).hasSize(1);
    }

    private static ImmutableSampleArgument sample() {
        return ImmutableSampleArgument.builder().id("sample").build();
    }

    private KubernetesCluster victimise(PlatinumConfiguration configuration) {
        return new KubernetesCluster("test",
                scheduler,
                "platinum-sa",
                pipelineInputs,
                configMaps,
                "output",
                "sa@gcloud.com",
                configuration,
                TargetNodePool.defaultPool());
    }
}