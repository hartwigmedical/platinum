package com.hartwig.platinum.config;

import org.junit.Before;

public class PlatinumConfigurationTest {
    private String runName;
    private String location;
    private String dataDirectory;

    @Before
    public void setup() {
        runName = "somerun";
        location = "en-us";
        dataDirectory = "/tmp";
    }

//    @Test
//    public void shouldCreateOutputConfigurationFromArgumentsReusingCmekFromPipeline() {
//        dataDirectory = "/tmp";
//        PlatinumConfiguration victim = PlatinumConfiguration.from(runName, resource("valid_input"), dataDirectory, location);
//        String cmek = "/some/cmek/key";
//        assertThat(victim.pipelineArguments()).isEqualTo(
//                Map.of("cmek", cmek, "some", "other arg"));
//        assertThat(victim.outputConfiguration().cmek()).isEqualTo(Optional.of(cmek));
//        assertThat(victim.outputConfiguration().location()).isEqualTo(location);
//        assertThat(victim.dataDirectory()).isEqualTo(dataDirectory);
//    }
//
//    @Test
//    public void shouldDefaultToEmptyOptionalForCmek() {
//        PlatinumConfiguration victim = PlatinumConfiguration.from(runName, resource("no_cmek"), dataDirectory, location);
//        assertThat(victim.outputConfiguration().cmek()).isEqualTo(Optional.empty());
//    }
//
//    private String resource(String basename) {
//        return System.getProperty("user.dir") + "/src/test/resources/platinum-configuration-test/" + basename + ".json";
//    }
}