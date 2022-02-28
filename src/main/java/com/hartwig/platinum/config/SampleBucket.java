package com.hartwig.platinum.config;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;

public class SampleBucket {

    private static final String TUMOR_PATH = "TUMOR";
    private static final String NORMAL_PATH = "NORMAL";
    public static final String R1 = "_R1";
    public static final String R2 = "_R2";
    private final Bucket bucket;

    public SampleBucket(final Bucket bucket) {
        this.bucket = bucket;
    }

    public List<SampleConfiguration> apply() {
        return StreamSupport.stream(bucket.list(Storage.BlobListOption.currentDirectory()).iterateAll().spliterator(), false)
                .map(blob -> ImmutableSampleConfiguration.builder()
                        .addTumors(extractFastq(blob, TUMOR_PATH))
                        .normal(extractFastq(blob, NORMAL_PATH))
                        .name(blob.getName().replace("/", ""))
                        .build())
                .collect(Collectors.toList());
    }

    private RawDataConfiguration extractFastq(final Blob blob, final String typePrefix) {
        final String sampleName =
                StreamSupport.stream(bucket.list(Storage.BlobListOption.prefix(blob.getName() + typePrefix)).iterateAll().spliterator(),
                        false).findFirst().map(BlobInfo::getName).map(s -> s.split("/")).map(s -> s[2]).orElseThrow();
        final Iterable<Blob> fastqs = bucket.list(Storage.BlobListOption.prefix(blob.getName() + typePrefix)).iterateAll();
        ImmutableRawDataConfiguration.Builder rawDataConfigurationBuilder = ImmutableRawDataConfiguration.builder();
        for (Blob fastq : fastqs) {
            if (fastq.getName().contains(R1)) {
                for (Blob pairCandidate : fastqs) {
                    if (isMatchingR2(fastq, pairCandidate)) {
                        rawDataConfigurationBuilder.addFastq(ImmutableFastqConfiguration.builder()
                                .read1(bucket.getName() + "/" + fastq.getName())
                                .read2(bucket.getName() + "/" + pairCandidate.getName())
                                .build());
                    }
                }
            }
        }
        return rawDataConfigurationBuilder.name(sampleName).build();
    }

    private boolean isMatchingR2(final Blob fastq, final Blob pairCandidate) {
        return pairCandidate.getName().contains(R2) && pairCandidate.getName().replace(R2, "").equals(fastq.getName().replace(R1, ""));
    }
}
