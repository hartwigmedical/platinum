package com.hartwig.platinum.p5sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.hartwig.platinum.config.FastqConfiguration;
import com.hartwig.platinum.config.RawDataConfiguration;
import com.hartwig.platinum.config.SampleConfiguration;

public class DecomposeSamples {

    public static List<TumorNormalPair> apply(final List<SampleConfiguration> configurations) {
        List<TumorNormalPair> pairs = new ArrayList<>();
        for (SampleConfiguration sample : configurations) {
            if (sample.name().length() >= 55) {
                throw new IllegalArgumentException("Platinum can only support sample names up to 55 characters. Please shorten the sample "
                        + "name (this name will not be used for file naming or in any headers)");
            }
            boolean indexTumors = sample.tumors().size() > 1;
            int tumorIndex = 1;
            for (RawDataConfiguration tumor : sample.tumors()) {
                pairs.add(ImmutableTumorNormalPair.builder()
                        .reference(ImmutableSample.builder().name(sample.normal().name()).lanes(toLanes(sample.normal().fastq())).build())
                        .tumor(ImmutableSample.builder().name(tumor.name()).lanes(toLanes(tumor.fastq())).build())
                        .tumorIndex(indexTumors ? Optional.of(tumorIndexString(tumorIndex)) : Optional.empty())
                        .name(indexTumors ? sample.name() + "-" + tumorIndexString(tumorIndex) : sample.name())
                        .build());
                tumorIndex++;
            }
        }
        return pairs;
    }

    public static String tumorIndexString(final int tumorIndex) {
        return "t" + tumorIndex;
    }

    private static List<ImmutableLane> toLanes(final List<FastqConfiguration> referenceFastq) {
        return IntStream.rangeClosed(1, referenceFastq.size())
                .boxed()
                .map(i -> Map.entry(String.valueOf(i), referenceFastq.get(i - 1)))
                .map(e -> ImmutableLane.builder()
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
