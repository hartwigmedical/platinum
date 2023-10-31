package com.hartwig.platinum.config.version;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

public class PipelineVersion {
    String pad(final String candidate) {
        ArrayList<String> versionsGiven = new ArrayList<>(List.of(candidate.split("\\.")));
        int i;
        int numMissing = 3 - versionsGiven.size();
        for (i = 0; i < numMissing; i++) {
            versionsGiven.add("0");
        }
        return String.join(".", versionsGiven);
    }

    String extract(final String fullPipelineVersion) {
        List<String> tokens = List.of(fullPipelineVersion.split(":"));
        if (tokens.size() == 1) {
            throw new IllegalArgumentException(format("Could not find version string in pipeline Docker image name [%s]",
                    fullPipelineVersion));
        } else {
            return tokens.get(tokens.size() - 1);
        }
    }
}
