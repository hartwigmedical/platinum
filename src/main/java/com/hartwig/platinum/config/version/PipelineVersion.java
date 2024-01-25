package com.hartwig.platinum.config.version;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

public class PipelineVersion {
    String padVersionStringToSemanticVersion(final String inputCandidate) {
        String candidate = inputCandidate.replaceAll("-(alpha|beta).[0-9]+$", "");
        ArrayList<String> versionsGiven = new ArrayList<>(List.of(candidate.split("\\.")));
        if (versionsGiven.size() > 3) {
            throw new IllegalArgumentException(format("Unrecognised pipeline version string [%s]", candidate));
        }
        int i;
        int numMissing = 3 - versionsGiven.size();
        for (i = 0; i < numMissing; i++) {
            versionsGiven.add("0");
        }
        return String.join(".", versionsGiven);
    }

    String extractVersionFromDockerImageName(final String pipelineDockerImageName) {
        List<String> tokens = List.of(pipelineDockerImageName.split(":"));
        if (tokens.size() == 1) {
            throw new IllegalArgumentException(format("Could not find version string in pipeline Docker image name [%s]",
                    pipelineDockerImageName));
        } else {
            return tokens.get(tokens.size() - 1);
        }
    }
}
