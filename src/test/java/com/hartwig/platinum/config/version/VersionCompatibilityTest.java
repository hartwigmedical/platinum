package com.hartwig.platinum.config.version;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class VersionCompatibilityTest {
    private PipelineVersion pipelineVersion;
    private VersionCompatibility versionCompatibility;

    @Before
    public void setup() {
        pipelineVersion = mock(PipelineVersion.class);
        when(pipelineVersion.padVersionStringToSemanticVersion(anyString())).thenAnswer(i -> i.getArguments()[0]);
        when(pipelineVersion.extractVersionFromDockerImageName(anyString())).thenAnswer(i -> i.getArguments()[0]);
        versionCompatibility = new VersionCompatibility("1.0.0", "2.0.0", pipelineVersion);
    }

    @Test
    public void shouldAcceptInRangeVersion() {
        versionCompatibility.check("1.1.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectTooOldVersion() {
        versionCompatibility.check("0.5.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectTooNewVersion() {
        versionCompatibility.check("3.0.0");
    }

    @Test
    public void shouldAcceptArbitrarilyOldVersionWhenMinSetToSmallest() {
        new VersionCompatibility(VersionCompatibility.SMALLEST_VERSION, "2.0.0", pipelineVersion).check("0.0.1");
    }

    @Test
    public void shouldAcceptArbitrarilyNewVersionWhenNoMaxIsSetToLargest() {
        new VersionCompatibility("1.0.0", VersionCompatibility.UNLIMITED, pipelineVersion).check("10000.10000.5324");
    }
}