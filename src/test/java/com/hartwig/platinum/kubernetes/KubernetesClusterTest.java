package com.hartwig.platinum.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.hartwig.pdl.PipelineInput;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.ImmutableGcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.kubernetes.pipeline.ImmutableSampleArgument;
import com.hartwig.platinum.kubernetes.pipeline.PipelineConfigMapBuilder;
import com.hartwig.platinum.kubernetes.pipeline.PipelineJob;
import com.hartwig.platinum.kubernetes.pipeline.SampleArgument;
import com.hartwig.platinum.scheduling.JobScheduler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

public class KubernetesClusterTest {
    private static final ImmutableGcpConfiguration GCP = GcpConfiguration.builder().project("project").region("region").build();
    private static final String SECRET = "secret";
    private static final String CONFIG = "config";
    private static final List<SampleArgument> SAMPLES = List.of(sample());
    private Map<String, Future<PipelineInput>> pipelineInputs;
    private KubernetesCluster victim;
    private JobScheduler scheduler;
    private Volume secret;
    private PipelineConfigMapBuilder configMaps;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        secret = new VolumeBuilder().withName(SECRET).build();
        scheduler = mock(JobScheduler.class);
        configMaps = mock(PipelineConfigMapBuilder.class);
        Future<PipelineInput> future1 = mock(Future.class);
        PipelineInput input1 = PipelineInput.builder().setName("setName").build();
        pipelineInputs = Map.of("sample1", future1);
        when(future1.get()).thenReturn(input1);
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
    @SuppressWarnings("unchecked")
    public void processesJobsInOrderBasedOnPipelineInputKeys() throws Exception {
        Future<PipelineInput> futureA = mock(Future.class);
        Future<PipelineInput> futureB = mock(Future.class);
        Future<PipelineInput> futureK = mock(Future.class);
        Future<PipelineInput> futureZ = mock(Future.class);
        PipelineInput inputA = PipelineInput.builder().setName("set a").build();
        PipelineInput inputB = PipelineInput.builder().setName("set b").build();
        PipelineInput inputK = PipelineInput.builder().setName("set k").build();
        PipelineInput inputZ = PipelineInput.builder().setName("set z").build();
        when(futureA.get()).thenReturn(inputA);
        when(futureB.get()).thenReturn(inputB);
        when(futureK.get()).thenReturn(inputK);
        when(futureZ.get()).thenReturn(inputZ);

        pipelineInputs = Map.of("z", futureZ, "a", futureA, "k", futureK, "b", futureB);
        victimise(PlatinumConfiguration.builder().keystorePassword("changeit").gcp(GCP).build()).submit();
        Thread.sleep(250);

        InOrder inOrder = inOrder(configMaps);
        inOrder.verify(configMaps).forSample("a", inputA);
        inOrder.verify(configMaps).forSample("b", inputB);
        inOrder.verify(configMaps).forSample("k", inputK);
        inOrder.verify(configMaps).forSample("z", inputZ);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void addsConfigMapAndSecretVolumes() {
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victim = victimise(PlatinumConfiguration.builder().gcp(GCP).build());
        victim.submit();
        verify(scheduler).submit(job.capture());
        assertThat(job.getValue().getVolumes()).extracting(Volume::getName).containsExactly(CONFIG, SECRET);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void addsConfigMapForSampleToEachJob() throws Exception {
        Future<PipelineInput> futureA = mock(Future.class);
        Future<PipelineInput> futureB = mock(Future.class);
        PipelineInput inputA = PipelineInput.builder().setName("set a").build();
        PipelineInput inputB = PipelineInput.builder().setName("set b").build();
        pipelineInputs = Map.of("sample-a", futureA, "sample-b", futureB);

        when(futureA.get()).thenReturn(inputA);
        when(futureB.get()).thenReturn(inputB);
        when(configMaps.forSample("sample-a", inputA)).thenReturn(new VolumeBuilder().withName("config-a").build());
        when(configMaps.forSample("sample-b", inputB)).thenReturn(new VolumeBuilder().withName("config-b").build());
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

    private static ImmutableSampleArgument sample() {
        return ImmutableSampleArgument.builder().id("sample").build();
    }

    private KubernetesCluster victimise(PlatinumConfiguration configuration) {
        return new KubernetesCluster("test",
                scheduler,
                () -> secret,
                pipelineInputs,
                configMaps,
                "output",
                "sa@gcloud.com",
                configuration,
                TargetNodePool.defaultPool());
    }
}