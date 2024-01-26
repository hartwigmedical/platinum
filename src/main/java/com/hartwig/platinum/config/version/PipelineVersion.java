package com.hartwig.platinum.config.version;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PipelineVersion {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-((?:alpha|beta)(?:\\.[0-9]+)*))?$");
    String padVersionStringToSemanticVersion(final String candidate) {
        Matcher matcher = VERSION_PATTERN.matcher(candidate);
        ArrayList<String> versionsGiven;

        if (matcher.find()) {
            versionsGiven = new ArrayList<>(List.of(matcher.group(1), matcher.group(2), matcher.group(3)));
        } else {
            versionsGiven = new ArrayList<>(List.of(candidate.split("\\.")));
        }

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
