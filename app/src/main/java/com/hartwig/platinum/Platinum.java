package com.hartwig.platinum;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import com.google.container.v1.Cluster;

public class Platinum {
    private String clusterName;
    private String patientsFile;

    Platinum(String clusterName, final String patientsFile) {
        this.clusterName = clusterName;
        this.patientsFile = patientsFile;
    }

    public void execute() {
        Cluster cluster = Cluster.newBuilder().setName(clusterName).build();
    }

    private Collection<String> readPatients(String filename) {
        try {
            return Set.copyOf(Files.readAllLines(Path.of(patientsFile)));
        } catch (IOException ioe) {
            throw new RuntimeException(format("Could not read patients list from [%s]", patientsFile));
        }
    }
}
