package com.hartwig.platinum.pdl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.hartwig.pdl.LaneInput;
import com.hartwig.pdl.PipelineInput;
import com.hartwig.pdl.SampleInput;
import com.hartwig.platinum.config.FastqConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.config.RawDataConfiguration;
import com.hartwig.platinum.config.SampleConfiguration;

public class ConvertFromGCSPaths implements PDLConversion {

    public static final int GCP_VM_LIMIT = 55;

    public Map<String, Future<PipelineInput>> apply(final PlatinumConfiguration configuration) {
        Map<String, Future<PipelineInput>> futures = new HashMap<>();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        for (SampleConfiguration sample : configuration.samples()) {
            if (sample.name().length() >= GCP_VM_LIMIT) {
                throw new IllegalArgumentException(
                        "Platinum can only support sample names up to " + GCP_VM_LIMIT + " characters. Please shorten the sample "
                                + "name (this name will not be used for file naming or in any headers)");
            }
            boolean indexTumors = sample.tumors().size() > 1;
            AtomicInteger tumorIndex = new AtomicInteger(1);
            if (!sample.tumors().isEmpty()) {
                for (RawDataConfiguration tumor : sample.tumors()) {
                    if (tumor.bam().isPresent()) {
                        String name = indexTumors ? sample.name() + "-t" + tumorIndex.get() : sample.name();
                        futures.put(name, executorService.submit(() -> PipelineInput.builder()
                                .reference(sample.normal().map(n -> SampleInput.builder().name(n.name()).bam(n.bam()).build()))
                                .tumor(SampleInput.builder()
                                        .name(tumor.name())
                                        .bam(tumor.bam())
                                        .primaryTumorDoids(sample.primaryTumorDoids())
                                        .build())
                                .setName(name)
                                .build()));
                    } else {
                        String name = indexTumors ? sample.name() + "-t" + tumorIndex : sample.name();
                        futures.put(name, executorService.submit(() -> PipelineInput.builder()
                                .reference(sample.normal().map(n -> SampleInput.builder().name(n.name()).lanes(toLanes(n.fastq())).build()))
                                .tumor(SampleInput.builder()
                                        .name(tumor.name())
                                        .lanes(toLanes(tumor.fastq()))
                                        .primaryTumorDoids(sample.primaryTumorDoids())
                                        .build())
                                .setName(name)
                                .build()));
                    }
                    tumorIndex.incrementAndGet();
                }
            } else if (sample.normal().isPresent()) {
                futures.put(sample.name(), executorService.submit(() ->PipelineInput.builder()
                        .reference(sample.normal().map(n -> SampleInput.builder().name(n.name()).lanes(toLanes(n.fastq())).build()))
                        .setName(sample.name())
                        .build()));
            }
        }
        return futures;
    }

    private static List<LaneInput> toLanes(final List<FastqConfiguration> referenceFastq) {
        return IntStream.rangeClosed(1, referenceFastq.size())
                .boxed()
                .map(i -> Map.entry(String.valueOf(i), referenceFastq.get(i - 1)))
                .map(e -> LaneInput.builder()
                        .laneNumber(e.getKey())
                        .firstOfPairPath(stripGs(e.getValue().read1()))
                        .secondOfPairPath(stripGs(e.getValue().read2()))
                        .build())
                .collect(Collectors.toList());
    }

    private static String stripGs(final String maybeWithGs) {
        return maybeWithGs.replace("gs://", "");
    }
}
