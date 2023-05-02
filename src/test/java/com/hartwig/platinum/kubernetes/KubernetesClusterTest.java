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
import com.hartwig.platinum.config.ImmutableBatchConfiguration;
import com.hartwig.platinum.config.ImmutableGcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;

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
    private KubernetesCluster victim;
    private JobScheduler scheduler;
    private Volume secret;
    private Delay delay;
    private PipelineConfigMaps configMaps;

    @Before
    public void setUp() {
        secret = new VolumeBuilder().withName(SECRET).build();
        scheduler = mock(JobScheduler.class);
        when(scheduler.submit(any())).thenReturn(true);
        delay = mock(Delay.class);
        configMaps = mock(PipelineConfigMaps.class);
        when(configMaps.forSample(any())).thenReturn(new VolumeBuilder().withName(CONFIG).build());
    }

    @Test
    public void addsJksVolumeAndContainerIfPasswordSpecified() {
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victim = new KubernetesCluster("test",
                scheduler,
                () -> secret,
                configMaps,
                "output",
                "sa@gcloud.com",
                PlatinumConfiguration.builder().keystorePassword("changeit").gcp(GCP).build(),
                delay,
                TargetNodePool.defaultPool());
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
    public void addsConfigMapAndSecretVolumes() {
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victim = new KubernetesCluster("test",
                scheduler,
                () -> secret,
                configMaps,
                "output",
                "sa@gcloud.com",
                PlatinumConfiguration.builder().gcp(GCP).build(),
                delay,
                TargetNodePool.defaultPool());
        mockConfigmapContents("sample");
        victim.submit();
        verify(scheduler).submit(job.capture());
        PipelineJob result = job.getValue();
        assertThat(result.getVolumes()).extracting(Volume::getName).containsExactly(CONFIG, SECRET);
    }

    @Test
    public void addsConfigMapForSampleToEachJob() {
        when(configMaps.getConfigMapKeys()).thenReturn(Set.of("sample-a", "sample-b"));
        when(configMaps.forSample("sample-a")).thenReturn(new VolumeBuilder().withName("config-a").build());
        when(configMaps.forSample("sample-b")).thenReturn(new VolumeBuilder().withName("config-b").build());
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victim = new KubernetesCluster("test",
                scheduler,
                () -> secret,
                configMaps,
                "output",
                "sa@gcloud.com",
                PlatinumConfiguration.builder().gcp(GCP).build(),
                delay,
                TargetNodePool.defaultPool());
        victim.submit();
        verify(scheduler, times(2)).submit(job.capture());
        List<PipelineJob> allJobs = job.getAllValues();
        List<PipelineJob> jobsA = allJobs.stream().filter(j -> j.getName().equals("sample-a")).collect(Collectors.toList());
        assertThat(jobsA.size()).isEqualTo(1);
        assertThat(jobsA.get(0).getVolumes().stream().filter(v -> v.getName().equals("config-a")).collect(Collectors.toList())).hasSize(1);
        List<PipelineJob> jobsB = allJobs.stream().filter(j -> j.getName().equals("sample-b")).collect(Collectors.toList());
        assertThat(jobsB.size()).isEqualTo(1);
        assertThat(jobsB.get(0).getVolumes().stream().filter(v -> v.getName().equals("config-b")).collect(Collectors.toList())).hasSize(1);
    }

    @Test
    public void runsInBatchesWithDelayWhenConfigured() {
        victim = new KubernetesCluster("test",
                scheduler,
                () -> secret,
                configMaps,
                "output",
                "sa@gcloud.com",
                PlatinumConfiguration.builder().gcp(GCP).batch(ImmutableBatchConfiguration.builder().size(2).delay(1).build()).build(),
                delay,
                TargetNodePool.defaultPool());
        mockConfigmapContents("1", "2", "3", "4", "5");
        victim.submit();

        ArgumentCaptor<Integer> delayCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(delay, times(2)).forMinutes(delayCaptor.capture());
        assertThat(delayCaptor.getAllValues()).hasSize(2);
        assertThat(delayCaptor.getAllValues().get(0)).isEqualTo(1);
        assertThat(delayCaptor.getAllValues().get(1)).isEqualTo(1);
    }

    private void mockConfigmapContents(String... keys) {
        var contents = Arrays.stream(keys).collect(Collectors.toSet());
        when(configMaps.getConfigMapKeys()).thenReturn(contents);
    }
}