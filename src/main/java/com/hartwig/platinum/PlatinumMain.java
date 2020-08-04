package com.hartwig.platinum;

import java.util.concurrent.Callable;

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

    @Override
    public Integer call() {
        PlatinumResult result = new Platinum(runName, inputJson).run();
        return result.numFailure() > 0 ? 1 : 0;
    }
}
