package com.hartwig.platinum;

import java.util.concurrent.Callable;

<<<<<<< HEAD
import com.google.cloud.storage.StorageOptions;

=======
import picocli.CommandLine;
>>>>>>> 998bb93... DEV-1436 Args and samples to JSON
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

    @Override
    public Integer call() {
        PlatinumResult result = new Platinum(runName, inputJson, StorageOptions.getDefaultInstance().getService()).run();
        return result.numFailure() > 0 ? 1 : 0;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new PlatinumMain()).execute(args));
    }
}
