package com.hartwig.platinum.kubernetes;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRunner.class);

    boolean execute(List<String> arguments) {
        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        try {
            LOGGER.info("Starting [{}]", arguments);
            Process process = processBuilder.start();
            process.waitFor();
            LOGGER.info("Process [{}] complete with exit code {}", arguments, process.exitValue());
            return process.exitValue() == 0;
        } catch (Exception e) {
            LOGGER.warn("Unable to run [{}]", arguments, e);
            return false;
        }
    }
}
