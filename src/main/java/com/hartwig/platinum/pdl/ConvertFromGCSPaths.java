package com.hartwig.platinum.pdl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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

    public List<Supplier<PipelineInput>> apply(final PlatinumConfiguration configuration) {
        List<Supplier<PipelineInput>> pairs = new ArrayList<>();
        for (SampleConfiguration sample : configuration.samples()) {
            if (sample.name().length() >= GCP_VM_LIMIT) {
                throw new IllegalArgumentException(
                        "Platinum can only support sample names up to " + GCP_VM_LIMIT + " characters. Please shorten the sample "
                                + "name (this name will not be used for file naming or in any headers)");
            }
            boolean indexTumors = sample.tumors().size() > 1;
            int tumorIndex = 1;
            if (!sample.tumors().isEmpty()) {
                for (RawDataConfiguration tumor : sample.tumors()) {
                    final int tumorIndexReadOnly = tumorIndex;
                    if (tumor.bam().isPresent()) {
                        pairs.add(() -> PipelineInput.builder()
                                .reference(sample.normal().map(n -> SampleInput.builder().name(n.name()).bam(n.bam()).build()))
                                .tumor(SampleInput.builder()
                                        .name(tumor.name())
                                        .bam(tumor.bam())
                                        .primaryTumorDoids(sample.primaryTumorDoids())
                                        .build())
                                .setName(indexTumors ? sample.name() + "-t" + tumorIndexReadOnly : sample.name())
                                .build());
                    } else {

                        pairs.add(() -> PipelineInput.builder()
                                .reference(sample.normal().map(n -> SampleInput.builder().name(n.name()).lanes(toLanes(n.fastq())).build()))
                                .tumor(SampleInput.builder()
                                        .name(tumor.name())
                                        .lanes(toLanes(tumor.fastq()))
                                        .primaryTumorDoids(sample.primaryTumorDoids())
                                        .build())
                                .setName(indexTumors ? sample.name() + "-t" + tumorIndexReadOnly : sample.name())
                                .build());
                    }
                    tumorIndex++;
                }
            } else if (sample.normal().isPresent()) {
                pairs.add(() -> PipelineInput.builder()
                        .reference(sample.normal().map(n -> SampleInput.builder().name(n.name()).lanes(toLanes(n.fastq())).build()))
                        .setName(sample.name())
                        .build());
            }
        }
        return pairs;
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
