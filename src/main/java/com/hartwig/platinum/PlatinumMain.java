package com.hartwig.platinum;

import java.util.concurrent.Callable;

import picocli.CommandLine.Option;

public class PlatinumMain implements Callable<Integer> {
    @Option(names = {"-c", "--cluster-name"}, required=true, description = "Name to give the cluster that will be created")
    private String clusterName;

    @Option(names = {"-p", "--patients"}, required=true, description = "Text file containing patient names (eg CPCT12345678), one per line")
    private String patients;

    @Override
    public Integer call() {
        new Platinum(clusterName, patients).execute();
        return 0;
    }
}
