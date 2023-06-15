package com.hartwig.platinum.kubernetes.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collections;

import com.hartwig.platinum.kubernetes.TargetNodePool;

import org.junit.Test;

import io.fabric8.kubernetes.api.model.Container;

public class PipelineJobTest {

    @Test
    public void namesJobWithRunAndSample() {
        PipelineJob victim =
                new PipelineJob("sample-run", new Container(), Collections.emptyList(), TargetNodePool.defaultPool(), Duration.ZERO);
        assertThat(victim.getName()).isEqualTo("sample-run");
    }
}