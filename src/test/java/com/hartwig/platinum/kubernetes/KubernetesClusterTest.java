package com.hartwig.platinum.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.hartwig.pdl.PipelineInput;
import com.hartwig.pdl.SampleInput;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.ImmutableGcpConfiguration;
import com.hartwig.platinum.config.ImmutablePlatinumConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.config.ServiceAccountConfiguration;
import com.hartwig.platinum.kubernetes.pipeline.ImmutableSampleArgument;
import com.hartwig.platinum.kubernetes.pipeline.PipelineConfigMapBuilder;
import com.hartwig.platinum.kubernetes.pipeline.PipelineJob;
import com.hartwig.platinum.kubernetes.pipeline.SampleArgument;
import com.hartwig.platinum.scheduling.JobScheduler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

public class KubernetesClusterTest {
    private static final String CONFIG = "config";
    private static final String TUMOR_NAME = "tumor-name";
    private static final String REFERENCE_NAME = "reference-name";
    private static final List<SampleArgument> SAMPLES = List.of(sample());
    private List<Supplier<PipelineInput>> pipelineInputs;
    private KubernetesCluster victim;
    private JobScheduler scheduler;
    private PipelineConfigMapBuilder configMaps;
    private ImmutablePlatinumConfiguration.Builder configBuilder;
    private ArgumentCaptor<PipelineJob> jobCaptor;

    @Before
    public void setUp() {
        scheduler = mock(JobScheduler.class);
        configMaps = mock(PipelineConfigMapBuilder.class);
        SampleInput tumor = SampleInput.builder().name(TUMOR_NAME).turquoiseSubject(TUMOR_NAME).build();
        PipelineInput input1 = PipelineInput.builder().setName("setName").tumor(tumor).build();
        pipelineInputs = List.of(() -> input1);
        when(configMaps.forSample(any(), any())).thenReturn(new VolumeBuilder().withName(CONFIG).build());
        ServiceAccountConfiguration serviceAccountConfiguration =
                ServiceAccountConfiguration.builder().kubernetesServiceAccount("platinum-sa").gcpEmailAddress("e@mail.com").build();
        ImmutableGcpConfiguration gcpConfiguration = GcpConfiguration.builder().project("project").region("region").build();
        configBuilder = PlatinumConfiguration.builder().gcp(gcpConfiguration).serviceAccount(serviceAccountConfiguration);
        jobCaptor = ArgumentCaptor.forClass(PipelineJob.class);
    }

    @Test
    public void addsJksVolumeAndContainerIfPasswordSpecified() {
        victim = victimise(configBuilder.keystorePassword("changeit").build());
        victim.submit();
        verify(scheduler).submit(jobCaptor.capture());
        PipelineJob result = jobCaptor.getValue();
        assertThat(result.getVolumes()).extracting(Volume::getName).contains("jks");
        assertThat(result.getContainer().getEnv()).hasSize(1);
        EnvVar env = result.getContainer().getEnv().get(0);
        assertThat(env.getName()).isEqualTo("JAVA_OPTS");
        assertThat(env.getValue()).isEqualTo("-Djavax.net.ssl.keyStore=/jks/api.jks -Djavax.net.ssl.keyStorePassword=changeit");
    }

    @Test
    public void submitsJobsToTheScheduler() {
        victimise(configBuilder.keystorePassword("changeit").build()).submit();
        verify(scheduler).submit(jobCaptor.capture());
        assertThat(jobCaptor.getAllValues().size()).isEqualTo(SAMPLES.size());
    }

    @Test
    public void addsConfigMapAndSecretVolumes() {
        victim = victimise(configBuilder.build());
        victim.submit();
        verify(scheduler).submit(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getVolumes()).extracting(Volume::getName).containsExactly(CONFIG);
    }

    @Test
    public void addsConfigMapForSampleToEachJob() {
        String nameA = TUMOR_NAME + "-a";
        String nameB = TUMOR_NAME + "-b";
        PipelineInput inputA = PipelineInput.builder().setName("set-a").tumor(SampleInput.builder().name(nameA).turquoiseSubject(nameA).build()).build();
        PipelineInput inputB = PipelineInput.builder().setName("set-b").tumor(SampleInput.builder().name(nameB).turquoiseSubject(nameB).build()).build();
        pipelineInputs = List.of(() -> inputA, () -> inputB);

        when(configMaps.forSample(nameA, inputA)).thenReturn(new VolumeBuilder().withName("config-a").build());
        when(configMaps.forSample(nameB, inputB)).thenReturn(new VolumeBuilder().withName("config-b").build());
        victimise(configBuilder.build()).submit();
        verify(scheduler, times(2)).submit(jobCaptor.capture());
        List<PipelineJob> allJobs = jobCaptor.getAllValues();
        List<PipelineJob> jobsA = allJobs.stream().filter(j -> j.getName().equals(nameA + "-test")).collect(Collectors.toList());
        assertThat(jobsA.size()).isEqualTo(1);
        assertThat(jobsA.get(0).getVolumes().stream().filter(v -> v.getName().equals("config-a")).collect(Collectors.toList())).hasSize(1);
        List<PipelineJob> jobsB = allJobs.stream().filter(j -> j.getName().equals(nameB + "-test")).collect(Collectors.toList());
        assertThat(jobsB.size()).isEqualTo(1);
        assertThat(jobsB.get(0).getVolumes().stream().filter(v -> v.getName().equals("config-b")).collect(Collectors.toList())).hasSize(1);
    }

    @Test
    public void usesReferenceSampleNameWhenTumorSampleUnspecified() {
        PipelineInput input = PipelineInput.builder().setName("set").reference(SampleInput.builder().name(REFERENCE_NAME).turquoiseSubject(REFERENCE_NAME).build()).build();
        pipelineInputs = List.of(() -> input);

        victimise(configBuilder.build()).submit();
        verify(scheduler).submit(jobCaptor.capture());
        assertThat(jobCaptor.getAllValues().size()).isEqualTo(1);
        PipelineJob job = jobCaptor.getValue();
        assertThat(job.getName()).isEqualTo(REFERENCE_NAME + "-test");
    }

    @Test
    public void usesTumorSampleNameWhenBothSamplesSpecified() {
        PipelineInput input = PipelineInput.builder().setName("set").reference(SampleInput.builder().name(REFERENCE_NAME).turquoiseSubject(REFERENCE_NAME).build())
                .tumor(SampleInput.builder().name(TUMOR_NAME).turquoiseSubject(TUMOR_NAME).build()).build();
        pipelineInputs = List.of(() -> input);

        victimise(configBuilder.build()).submit();
        verify(scheduler).submit(jobCaptor.capture());
        assertThat(jobCaptor.getAllValues().size()).isEqualTo(1);
        PipelineJob job = jobCaptor.getValue();
        assertThat(job.getName()).isEqualTo(TUMOR_NAME + "-test");
    }

    @Test
    public void ignoresSampleWhenNeitherTumorNorReferenceSpecified() {
        PipelineInput input = PipelineInput.builder().setName("set").build();
        pipelineInputs = List.of(() -> input);

        victimise(configBuilder.build()).submit();
        verify(scheduler, never()).submit(any());
    }

    private static ImmutableSampleArgument sample() {
        return ImmutableSampleArgument.builder().id("sample").build();
    }

    private KubernetesCluster victimise(PlatinumConfiguration configuration) {
        return new KubernetesCluster("test",
                scheduler,
                pipelineInputs,
                configMaps,
                "output-bucket",
                configuration,
                TargetNodePool.defaultPool());
    }
}