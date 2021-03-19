package com.hartwig.platinum.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.ImmutableGcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

public class KubernetesClusterTest {

    private static final List<SampleArgument> SAMPLES = List.of(ImmutableSampleArgument.builder().id("sample").build());
    private static final ImmutableGcpConfiguration GCP = GcpConfiguration.builder().project("project").region("region").build();
    private static final String SECRET = "secret";
    private static final String CONFIG = "config";
    private KubernetesCluster victim;
    private JobScheduler scheduler;
    private Volume secret;
    private Volume configMap;

    @Before
    public void setUp() {
        secret = new VolumeBuilder().withName(SECRET).build();
        configMap = new VolumeBuilder().withName(CONFIG).build();
        scheduler = mock(JobScheduler.class);
    }

    @Test
    public void addsJksVolumeAndContainerIfPasswordSpecified() {
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victim = new KubernetesCluster("test",
                scheduler,
                () -> secret,
                () -> configMap,
                "output",
                "sa@gcloud.com",
                PlatinumConfiguration.builder().keystorePassword("changeit").gcp(GCP).build());
        victim.submit(List.of(ImmutableSampleArgument.builder().id("sample").build()));
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
                () -> configMap,
                "output",
                "sa@gcloud.com",
                PlatinumConfiguration.builder().gcp(GCP).build());
        victim.submit(SAMPLES);
        verify(scheduler).submit(job.capture());
        PipelineJob result = job.getValue();
        assertThat(result.getVolumes()).extracting(Volume::getName).containsExactly(CONFIG, SECRET);
    }
}