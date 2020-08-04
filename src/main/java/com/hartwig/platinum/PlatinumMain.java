package com.hartwig.platinum;

import java.util.concurrent.Callable;

import com.google.cloud.storage.StorageOptions;
import com.hartwig.platinum.iam.IamProvider;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class PlatinumMain implements Callable<Integer> {

    @Option(names = { "-r", "--run_name" },
            required = true,
            description = "The name of the run, used in output bucket and cluster naming")
    private String runName;

    @Option(names = { "-i", "--input_json" },
            required = true,
            description = "")
    private String inputJson;

    @Option(names = { "-p", "--project" },
            required = true,
            description = "")
    private String project;

    @Override
    public Integer call() {
        PlatinumResult result = new Platinum(runName, inputJson, StorageOptions.getDefaultInstance().getService(), IamProvider.get(),
                project).run();
        return result.numFailure() > 0 ? 1 : 0;
    }

    public static void main(final String[] args) {
        int exitCode = new CommandLine(new PlatinumMain()).execute(args);
        System.exit(exitCode);
    }
}
