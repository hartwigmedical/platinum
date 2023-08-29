package com.hartwig.platinum.pdl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.hartwig.pdl.LaneInput;
import com.hartwig.pdl.PipelineInput;
import com.hartwig.platinum.config.FastqConfiguration;
import com.hartwig.platinum.config.ImmutableFastqConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.config.RawDataConfiguration;
import com.hartwig.platinum.config.SampleConfiguration;
import com.hartwig.platinum.config.ServiceAccountConfiguration;

import org.junit.Test;

public class ConvertFromGCSPathsTest {

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentWhenSampleNameExceeds55Chars() {
        new ConvertFromGCSPaths().apply(createConfiguration(List.of(SampleConfiguration.builder()
                .name(stringOf(55))
                .normal(data("normal"))
                .build())));
    }

    @Test
    public void decomposesMultiNormalSamplesIntoPairs() {
        List<Supplier<PipelineInput>> pipelineInputs =
                new ConvertFromGCSPaths().apply(createConfiguration(List.of(SampleConfiguration.builder()
                        .name("sample")
                        .tumors(List.of(data("first_tumor"), data("second_tumor")))
                        .normal(data("normal"))
                        .build())));
        assertThat(pipelineInputs).hasSize(2);
        PipelineInput firstPair = pipelineInputs.get(0).get();
        assertThat(firstPair.setName()).isEqualTo("sample-t1");
        assertThat(firstPair.tumor().orElseThrow().name()).isEqualTo("first_tumor");
        assertThat(firstPair.reference().orElseThrow().name()).isEqualTo("normal");
        PipelineInput secondPair = pipelineInputs.get(1).get();
        assertThat(secondPair.setName()).isEqualTo("sample-t2");
        assertThat(secondPair.tumor().orElseThrow().name()).isEqualTo("second_tumor");
        assertThat(secondPair.reference().orElseThrow().name()).isEqualTo("normal");
    }

    @Test
    public void populatesTumorAndNormalLanes() {
        List<Supplier<PipelineInput>> pairs = new ConvertFromGCSPaths().apply(createConfiguration(List.of(SampleConfiguration.builder()
                .name("sample")
                .tumors(List.of(data("first_tumor",
                        ImmutableFastqConfiguration.builder().read1("first_tumor_read1.fastq").read2("first_tumor_read2.fastq").build())))
                .normal(data("normal",
                        ImmutableFastqConfiguration.builder().read1("normal_read1.fastq").read2("normal_read2.fastq").build()))
                .build())));
        PipelineInput pair = pairs.get(0).get();
        assertThat(pair.tumor().orElseThrow().lanes()).containsOnly(LaneInput.builder()
                .laneNumber("1")
                .firstOfPairPath("first_tumor_read1.fastq")
                .secondOfPairPath("first_tumor_read2.fastq")
                .build());
        assertThat(pair.reference().orElseThrow().lanes()).containsOnly(LaneInput.builder()
                .laneNumber("1")
                .firstOfPairPath("normal_read1.fastq")
                .secondOfPairPath("normal_read2.fastq")
                .build());
    }

    public RawDataConfiguration data(final String first_tumor, final FastqConfiguration... fastq) {
        return RawDataConfiguration.builder().name(first_tumor).addFastq(fastq).build();
    }

    public String stringOf(final int chars) {
        return IntStream.rangeClosed(0, chars - 1).boxed().map(i -> " ").collect(Collectors.joining());
    }

    private PlatinumConfiguration createConfiguration(List<SampleConfiguration> sampleConfigurations) {
        return PlatinumConfiguration.builder()
                .samples(sampleConfigurations)
                .serviceAccount(ServiceAccountConfiguration.builder().kubernetesServiceAccount("ksa").gcpEmailAddress("email").build())
                .build();
    }
}