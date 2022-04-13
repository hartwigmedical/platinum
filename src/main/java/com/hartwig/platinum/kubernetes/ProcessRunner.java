package com.hartwig.platinum.kubernetes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRunner.class);

    boolean execute(List<String> arguments) {
        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        try {
            LOGGER.debug("Starting [{}]", arguments);
            Process process = processBuilder.inheritIO().start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            process.waitFor();
            LOGGER.debug(reader.lines().collect(Collectors.joining("\n")));
            LOGGER.debug("Process [{}] complete with exit code {}", arguments, process.exitValue());
            return process.exitValue() == 0;
        } catch (Exception e) {
            LOGGER.warn("Unable to run [{}]", arguments, e);
            return false;
        }
    }
}
