package com.hartwig.platinum.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.Test;

import io.fabric8.kubernetes.api.model.Container;

public class PipelineJobTest {

    @Test
    public void namesJobWithRunAndSample() {
        PipelineJob victim = new PipelineJob("run", "sample", new Container(), Collections.emptyList(), TargetNodePool.defaultPool());
        assertThat(victim.getName()).isEqualTo("sample-run");
    }
}