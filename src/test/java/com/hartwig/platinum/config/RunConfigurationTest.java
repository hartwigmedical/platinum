package com.hartwig.platinum.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class RunConfigurationTest {
    private String runName;
    private String sampleOne;
    private String sampleTwo;

    @Before
    public void setup() {
        runName = "somerun";
        sampleOne = "CPCT12345678";
        sampleTwo = "CORE12345678";
    }

    @Test
    public void shouldParseValidInputJson() {
        assertValidJson("valid_input.json");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfSamplesNotSpecified() {
        RunConfiguration.from(runName, resource("no_samples.json"));
    }

    @Test
    public void shouldRemoveDuplicates() {
        assertValidJson("duplicate_samples.json");
    }

    private void assertValidJson(String resourceName) {
        RunConfiguration victim = RunConfiguration.from(runName, resource(resourceName));
        assertThat(victim.runName()).isEqualTo(runName);
        List<PipelineConfiguration> pipelines = victim.pipelines();
        assertThat(pipelines.size()).isEqualTo(2);

        Map<String, String> pipelineArgs = Map.of("some", "arg");

        PipelineConfiguration configOne = PipelineConfiguration.builder().sampleName(sampleOne).arguments(pipelineArgs).build();
        PipelineConfiguration configTwo = PipelineConfiguration.builder().sampleName(sampleTwo).arguments(pipelineArgs).build();

        assertThat(pipelines).containsExactlyInAnyOrder(configOne, configTwo);
        assertThat(pipelines).contains(configOne);
        assertThat(pipelines).contains(configTwo);

    }

    private String resource(String name) {
        return new File(System.getProperty("user.dir") + "/src/test/resources/run-configuration-test/" + name).getAbsolutePath();
    }
}