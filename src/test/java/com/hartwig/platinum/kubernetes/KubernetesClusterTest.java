package com.hartwig.platinum.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.ImmutableGcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.scheduling.JobScheduler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

public class KubernetesClusterTest {
    private static final ImmutableGcpConfiguration GCP = GcpConfiguration.builder().project("project").region("region").build();
    private static final String SECRET = "secret";
    private static final String CONFIG = "config";
    private static final List<SampleArgument> SAMPLES = List.of(sample("sample"));
    private KubernetesCluster victim;
    private JobScheduler scheduler;
    private Volume secret;
    private PipelineConfigMaps configMaps;

    @Before
    public void setUp() {
        secret = new VolumeBuilder().withName(SECRET).build();
        scheduler = mock(JobScheduler.class);
        configMaps = mock(PipelineConfigMaps.class);
        when(configMaps.forSample(any())).thenReturn(new VolumeBuilder().withName(CONFIG).build());
    }

    @Test
    public void addsJksVolumeAndContainerIfPasswordSpecified() {
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victim = victimise(PlatinumConfiguration.builder().keystorePassword("changeit").gcp(GCP).build());
        mockConfigmapContents("sample");
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
        mockConfigmapContents("sample");
        victimise(PlatinumConfiguration.builder().keystorePassword("changeit").gcp(GCP).build()).submit();
        verify(scheduler).submit(job.capture());
        assertThat(job.getAllValues().size()).isEqualTo(SAMPLES.size());
    }

    @Test
    public void addsConfigMapAndSecretVolumes() {
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victim = victimise(PlatinumConfiguration.builder().gcp(GCP).build());
        mockConfigmapContents("sample");
        victim.submit();
        verify(scheduler).submit(job.capture());
        assertThat(job.getValue().getVolumes()).extracting(Volume::getName).containsExactly(CONFIG, SECRET);
    }

    @Test
    public void addsConfigMapForSampleToEachJob() {
        when(configMaps.getSampleKeys()).thenReturn(Set.of("sample-a", "sample-b"));
        when(configMaps.forSample("sample-a")).thenReturn(new VolumeBuilder().withName("config-a").build());
        when(configMaps.forSample("sample-b")).thenReturn(new VolumeBuilder().withName("config-b").build());
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victimise(PlatinumConfiguration.builder().gcp(GCP).build()).submit();
        verify(scheduler, times(2)).submit(job.capture());
        List<PipelineJob> allJobs = job.getAllValues();
        List<PipelineJob> jobsA = allJobs.stream().filter(j -> j.getName().equals("sample-a")).collect(Collectors.toList());
        assertThat(jobsA.size()).isEqualTo(1);
        assertThat(jobsA.get(0).getVolumes().stream().filter(v -> v.getName().equals("config-a")).collect(Collectors.toList())).hasSize(1);
        List<PipelineJob> jobsB = allJobs.stream().filter(j -> j.getName().equals("sample-b")).collect(Collectors.toList());
        assertThat(jobsB.size()).isEqualTo(1);
        assertThat(jobsB.get(0).getVolumes().stream().filter(v -> v.getName().equals("config-b")).collect(Collectors.toList())).hasSize(1);
    }

    private void mockConfigmapContents(String... keys) {
        when(configMaps.getSampleKeys()).thenReturn(Arrays.stream(keys).collect(Collectors.toSet()));
    }

    private static ImmutableSampleArgument sample(final String id) {
        return ImmutableSampleArgument.builder().id(id).build();
    }

    private KubernetesCluster victimise(PlatinumConfiguration configuration) {
        return new KubernetesCluster("test",
                scheduler,
                () -> secret,
                configMaps,
                "output",
                "sa@gcloud.com",
                configuration,
                TargetNodePool.defaultPool());
    }
}