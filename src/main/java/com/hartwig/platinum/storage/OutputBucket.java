package com.hartwig.platinum.storage;

public interface OutputBucket {

    String path();

    static OutputBucket findOrCreate(final String runName) {
        // create the output bucket based on the runname
        return null;
    }
}
