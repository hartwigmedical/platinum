package com.hartwig.platinum.storage;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.hartwig.platinum.config.OutputConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputBucket {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutputBucket.class);

    private final Storage storage;

    private OutputBucket(final Storage storage) {
        this.storage = storage;
    }

    public static OutputBucket from(final Storage storage) {
        return new OutputBucket(storage);
    }

    public String findOrCreate(final String runName, final OutputConfiguration configuration) {
        String bucketName = BucketName.of(runName);
        Bucket outputBucket = storage.get(bucketName);
        if (outputBucket == null) {
            outputBucket = storage.create(BucketInfo.newBuilder(bucketName)
                    .setLocation(configuration.location())
                    .setDefaultKmsKeyName(configuration.cmek().orElse(null))
                    .build());
        } else {
            LOGGER.warn("Bucket [{}] already existed. Platinum will try to re-use this bucket but cannot guarantee location or CMEK "
                    + "settings.", bucketName);
        }
        return outputBucket.getName();
    }
}
