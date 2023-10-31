package com.hartwig.platinum.config.version;

import static java.lang.String.format;

import com.vdurmont.semver4j.Semver;

public class VersionCompatibility {
    public static final String UNLIMITED = "unlimited";
    public static final String SMALLEST_VERSION = "0.0.0";
    private static final String LARGEST_VERSION = format("%d.%d.%d", Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    private final Semver minimum;
    private final Semver maximum;
    private final String maximumForDisplay;
    private final PipelineVersion pipelineVersion;

    public VersionCompatibility(final String minInclusive, final String maxInclusive, final PipelineVersion pipelineVersion) {
        this.pipelineVersion = pipelineVersion;
        minimum = new Semver(pipelineVersion.pad(minInclusive));
        if (maxInclusive.equals(UNLIMITED)) {
            maximumForDisplay = maxInclusive;
            maximum = new Semver(LARGEST_VERSION);
        } else {
            maximumForDisplay = pipelineVersion.pad(maxInclusive);
            maximum = new Semver(maximumForDisplay);
        }
    }

    public void check(final String candidate) {
        String padded = pipelineVersion.pad(pipelineVersion.extract(candidate));
        if (!(minimum.isLowerThanOrEqualTo(padded) && maximum.isGreaterThanOrEqualTo(padded))) {
            throw new IllegalArgumentException(format("Pipeline5 version must be between [%s] and [%s] (requested [%s])",
                    minimum, maximumForDisplay, candidate));
        }
    }
}
