package com.hartwig.platinum.storage;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.hartwig.platinum.config.OutputConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputBucket {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutputBucket.class);
    private static final String REUSE_MESSAGE =
            "To reuse it either delete the " + "old bucket and re-run or update the configuration to match.";

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
            checkLocation(configuration, outputBucket);
            checkCmek(configuration, outputBucket);
            LOGGER.info("Bucket [{}] already existed in storage with matching location and CMEK, reusing.", outputBucket.getName());
        }
        return outputBucket.getName();
    }

    private static void checkCmek(final OutputConfiguration configuration, final Bucket outputBucket) {
        final String kmsKeyName = outputBucket.getDefaultKmsKeyName();
        if (configuration.cmek().isEmpty() && kmsKeyName != null) {
            throw cmekMismatchNoneConfigured(outputBucket);
        } else if (configuration.cmek().map(k -> !k.equals(kmsKeyName)).orElse(false)) {
            throw cmekMismatch(configuration, outputBucket);
        }
    }

    private static void checkLocation(final OutputConfiguration configuration, final Bucket outputBucket) {
        if (!configuration.location().equals(outputBucket.getLocation())) {
            throw locationMismatch(configuration, outputBucket);
        }
    }

    private static IllegalArgumentException cmekMismatch(final OutputConfiguration configuration, final Bucket outputBucket) {
        return new IllegalArgumentException(String.format(
                "Bucket [%s] has a CMEK key configured of [%s] which does not match the specified key of [%s]. ",
                outputBucket.getName(),
                outputBucket.getDefaultKmsKeyName(),
                configuration.cmek().orElse("")) + REUSE_MESSAGE);
    }

    private static IllegalArgumentException cmekMismatchNoneConfigured(final Bucket outputBucket) {
        return new IllegalArgumentException(String.format(
                "Bucket [%s] has a CMEK key configured of [%s] but no key was configured for this run. ",
                outputBucket.getName(),
                outputBucket.getDefaultKmsKeyName()) + REUSE_MESSAGE);
    }

    private static IllegalArgumentException locationMismatch(final OutputConfiguration configuration, final Bucket outputBucket) {
        return new IllegalArgumentException(String.format(
                "Bucket [%s] already exists in a different location [%s] than the specified location of [%s]. " + REUSE_MESSAGE,
                outputBucket.getName(),
                outputBucket.getLocation(),
                configuration.location()));
    }
}
