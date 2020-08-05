package com.hartwig.platinum;

import java.util.concurrent.Callable;

import com.google.cloud.storage.StorageOptions;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class PlatinumMain implements Callable<Integer> {

    @Option(names = { "-r", "--run_name" },
            required = true,
            description = "The name of the run, used in output bucket and cluster naming")
    private String runName;

    @Option(names = { "-i", "--input_json" },
            required = true,
            description = "JSON file that contains arguments to be passed to the pipeline jobs and a list of samples")
    private String inputJson;

    @Option(names = {"-d", "--data_directory"},
            required = true,
            description = "Directory containing the JSON manifests for the samples in the input")
    private String dataDirectory;

    @Option(names = {"-l", "--location"},
            required = true,
            description = "GCP location for the Kubernetes cluster to be created in")
    private String location;

    @Override
    public Integer call() {
        try {
            new Platinum(runName, inputJson, dataDirectory, location, StorageOptions.getDefaultInstance().getService()).run();
            return(0);
        } catch (Exception e) {
            e.printStackTrace();
            return(1);
        }
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new PlatinumMain()).execute(args));
    }
}
