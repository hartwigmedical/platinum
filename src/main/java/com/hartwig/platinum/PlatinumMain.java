package com.hartwig.platinum;

import java.util.concurrent.Callable;

import com.google.cloud.storage.StorageOptions;
import com.hartwig.platinum.iam.IamProvider;
import com.hartwig.platinum.iam.ResourceManagerProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class PlatinumMain implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatinumMain.class);

    @Option(names = { "-r", "--run_name" },
            required = true,
            description = "The name of the run, used in output bucket and cluster naming")
    private String runName;

    @Option(names = { "-i", "--input_json" },
            required = true,
            description = "JSON file that contains arguments to be passed to the pipeline jobs and a list of samples")
    private String inputJson;

    @Option(names = {"-k", "--key-file"},
            required = true,
            description = "A service account key file to be used for interactions with GCP")
    private String keyFile;

    @Option(names = { "-p", "--project" },
            required = true,
            description = "")
    private String project;

    @Override
    public Integer call() {
        try {
            new Platinum(runName,
                    inputJson,
                    keyFile,
                    StorageOptions.getDefaultInstance().getService(),
                    IamProvider.get(),
                    ResourceManagerProvider.get(),
                    project).run();
            return (0);
        } catch (Exception e) {
            LOGGER.error("Unexpected exception", e);
            return (1);
        }
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new PlatinumMain()).execute(args));
    }
}
