package com.hartwig.platinum.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;

import com.fasterxml.jackson.databind.node.TextNode;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.ImmutableGcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesClusterTest {

    private static final Map<String, TextNode> SAMPLES = Map.of("sample", new TextNode("test"));
    private static final ImmutableGcpConfiguration GCP = GcpConfiguration.builder().project("project").region("region").build();
    private static final String SECRET = "secret";
    private static final String CONFIG = "config";
    private KubernetesCluster victim;
    private JobScheduler scheduler;

    @Before
    public void setUp() {
        KubernetesClient kubernetesClient = mock(KubernetesClient.class);
        Volume secret = new VolumeBuilder().withName(SECRET).build();
        Volume configMap = new VolumeBuilder().withName(CONFIG).build();
        scheduler = mock(JobScheduler.class);
        victim = new KubernetesCluster("test", kubernetesClient, scheduler, () -> secret, () -> configMap, "output", "sa@gcloud.com");
    }

    @Test
    public void addsJksVolumeAndContainerIfPasswordSpecified() {
        ArgumentCaptor<PipelineJob> job = ArgumentCaptor.forClass(PipelineJob.class);
        victim.submit(PlatinumConfiguration.builder()
                .samples(SAMPLES)
                .keystorePassword("changeit")
                .gcp(GcpConfiguration.builder().project("project").region("region").build())
                .build());
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
        victim.submit(PlatinumConfiguration.builder().samples(SAMPLES).gcp(GCP).build());
        verify(scheduler).submit(job.capture());
        PipelineJob result = job.getValue();
        assertThat(result.getVolumes()).extracting(Volume::getName).containsExactly(CONFIG, SECRET);
    }
}