package com.hartwig.platinum.storage;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageRoles;
import com.hartwig.platinum.Console;
import com.hartwig.platinum.config.PlatinumConfiguration;

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

    public String findOrCreate(final String runName, final String region, final String serviceAccount,
            final PlatinumConfiguration configuration) {
        String bucketName = BucketName.from(runName);
        Bucket outputBucket = storage.get(bucketName);
        if (outputBucket != null) {
            LOGGER.info("Deleting existing bucket {}", Console.bold(outputBucket.getName()));
            StreamSupport.stream(outputBucket.list().iterateAll().spliterator(), false).forEach(Blob::delete);
            storage.delete(bucketName);
        }
        outputBucket = storage.create(BucketInfo.newBuilder(bucketName)
                .setIamConfiguration(BucketInfo.IamConfiguration.newBuilder().setIsUniformBucketLevelAccessEnabled(true).build())
                .setLocation(region)
                .setDefaultKmsKeyName(configuration.cmek().orElse(null))
                .build());
        Policy bucketPolicy = storage.getIamPolicy(bucketName);
        storage.setIamPolicy(bucketName,
                bucketPolicy.toBuilder().addIdentity(StorageRoles.admin(), Identity.serviceAccount(serviceAccount)).build());
        return outputBucket.getName();
    }
}
