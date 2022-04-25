package com.hartwig.platinum.config;

import java.util.List;
import java.util.Optional;
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
                        .addAllTumors(extractFastq(blob, TUMOR_PATH).stream().collect(Collectors.toList()))
                        .normal(extractFastq(blob, NORMAL_PATH))
                        .name(blob.getName().replace("/", ""))
                        .build())
                .collect(Collectors.toList());
    }

    private Optional<RawDataConfiguration> extractFastq(final Blob blob, final String typePrefix) {
        final Optional<String> maybeSampleName =
                StreamSupport.stream(bucket.list(Storage.BlobListOption.prefix(blob.getName() + typePrefix)).iterateAll().spliterator(),
                        false).findFirst().map(BlobInfo::getName).map(s -> s.split("/")).map(s -> s[0]);
        ImmutableRawDataConfiguration.Builder rawDataConfigurationBuilder = ImmutableRawDataConfiguration.builder();
        if (maybeSampleName.isPresent()) {
            String sampleName = maybeSampleName.get();
            final Iterable<Blob> fastqs = bucket.list(Storage.BlobListOption.prefix(blob.getName() + typePrefix)).iterateAll();
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
            return Optional.of(rawDataConfigurationBuilder.name(sampleName).build());
        }
        return Optional.empty();
    }

    private boolean isMatchingR2(final Blob fastq, final Blob pairCandidate) {
        return pairCandidate.getName().contains(R2) && pairCandidate.getName().replace(R2, "").equals(fastq.getName().replace(R1, ""));
    }
}
