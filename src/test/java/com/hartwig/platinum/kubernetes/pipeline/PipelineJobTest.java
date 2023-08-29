package com.hartwig.platinum.kubernetes.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.hartwig.platinum.kubernetes.TargetNodePool;

import org.junit.Before;
import org.junit.Test;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.batch.JobSpec;

public class PipelineJobTest {
    private PipelineJob victim;

    @Before
    public void setup() {
        String name = "config-map";
        List<Volume> volumes = List.of(new VolumeBuilder().withName(name).editOrNewConfigMap().withName(name).endConfigMap().build());
        victim = new PipelineJob("sample-run", new Container(), volumes, "platinum-sa", TargetNodePool.defaultPool(), Duration.ZERO);
    }

    @Test
    public void namesJobWithRunAndSample() {
        assertThat(victim.getName()).isEqualTo("sample-run");
    }

    private PodSpec assertSpecNotNull() {
        JobSpec jobSpec = victim.asKubernetes();
        assertThat(jobSpec.getTemplate()).isNotNull();
        assertThat(jobSpec.getTemplate().getSpec()).isNotNull();
        return jobSpec.getTemplate().getSpec();
    }

    @Test
    public void setsServiceAccount() {
        PodSpec podSpec = assertSpecNotNull();
        assertThat(podSpec.getServiceAccountName()).isEqualTo("platinum-sa");
    }

    @Test
    public void willOnlyRunOnWorkloadIdentityEnabledNodes() {
        PodSpec podSpec = assertSpecNotNull();
        assertThat(podSpec.getNodeSelector()).isEqualTo(Map.of("iam.gke.io/gke-metadata-server-enabled", "true"));
    }

    @Test
    public void setsRestartPolicy() {
        assertThat(assertSpecNotNull().getRestartPolicy()).isEqualTo("Never");
    }

    @Test
    public void setsConfigMapVolume() {
        PodSpec podSpec = assertSpecNotNull();
        assertThat(podSpec.getVolumes()).isNotNull();
        ConfigMapVolumeSource configMap = podSpec.getVolumes().get(0).getConfigMap();
        assertThat(configMap).isNotNull();
        assertThat(configMap.getName()).isEqualTo("config-map");
    }
}