package com.hartwig.platinum.config.version;

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
        when(pipelineVersion.pad("1")).thenReturn("1.0.0");
        when(pipelineVersion.pad("2")).thenReturn("2.0.0");
        versionCompatibility = new VersionCompatibility("1", "2", pipelineVersion);
    }

    @Test
    public void shouldAcceptInRangeVersion() {
        assertAllows("1.1.0", true);
    }

    @Test
    public void shouldRejectTooOldVersion() {
        assertAllows("0.5.0", false);
    }

    @Test
    public void shouldRejectTooNewVersion() {
        assertAllows("3.0.0", false);
    }

    @Test
    public void shouldAcceptArbitrarilyOldVersionWhenMinSetToSmallest() {
        when(pipelineVersion.pad(VersionCompatibility.SMALLEST_VERSION)).thenReturn(VersionCompatibility.SMALLEST_VERSION);
        String version = "0.0.1";
        when(pipelineVersion.pad(version)).thenReturn(version);
        new VersionCompatibility(VersionCompatibility.SMALLEST_VERSION, "2", pipelineVersion).check(version);
    }

    @Test
    public void shouldAcceptArbitrarilyNewVersionWhenNoMaxIsSetToLargest() {
        String version = "10000.10000.5324";
        when(pipelineVersion.pad(version)).thenAnswer(i -> i.getArguments()[0]);
        new VersionCompatibility("1", VersionCompatibility.UNLIMITED, pipelineVersion).check(version);
    }

    private void assertAllows(String version, boolean allows) {
        when(pipelineVersion.pad(version)).thenReturn(version);
        if (allows) {
            versionCompatibility.check(version);
        } else {
            try {
                versionCompatibility.check(version);
            } catch (IllegalArgumentException e) {
                return;
            }
            throw new RuntimeException("Expected an IllegalArgumentException");
        }
    }
}