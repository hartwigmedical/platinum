package com.hartwig.platinum.p5sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.hartwig.platinum.config.FastqConfiguration;
import com.hartwig.platinum.config.ImmutableFastqConfiguration;
import com.hartwig.platinum.config.ImmutableRawDataConfiguration;
import com.hartwig.platinum.config.ImmutableSampleConfiguration;

import org.junit.Test;

public class DecomposeSamplesTest {

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentWhenSampleNameExceeds55Chars() {
        DecomposeSamples.apply(List.of(ImmutableSampleConfiguration.builder().name(stringOf(55)).normal(data("normal")).build()));
    }

    @Test
    public void decomposesMultiNormalSamplesIntoPairs() {
        List<TumorNormalPair> pairs = DecomposeSamples.apply(List.of(ImmutableSampleConfiguration.builder()
                .name("sample")
                .tumors(List.of(data("first_tumor"), data("second_tumor")))
                .normal(data("normal"))
                .build()));
        assertThat(pairs).hasSize(2);
        TumorNormalPair firstPair = pairs.get(0);
        assertThat(firstPair.name()).isEqualTo("sample-t1");
        assertThat(firstPair.tumor().name()).isEqualTo("first_tumor");
        assertThat(firstPair.reference().name()).isEqualTo("normal");
        assertThat(firstPair.tumorIndex()).hasValue("t1");
        TumorNormalPair secondPair = pairs.get(1);
        assertThat(secondPair.name()).isEqualTo("sample-t2");
        assertThat(secondPair.tumor().name()).isEqualTo("second_tumor");
        assertThat(secondPair.reference().name()).isEqualTo("normal");
        assertThat(secondPair.tumorIndex()).hasValue("t2");
    }

    @Test
    public void populatesTumorAndNormalLanes() {
        List<TumorNormalPair> pairs = DecomposeSamples.apply(List.of(ImmutableSampleConfiguration.builder()
                .name("sample")
                .tumors(List.of(data("first_tumor",
                        ImmutableFastqConfiguration.builder().read1("first_tumor_read1.fastq").read2("first_tumor_read2.fastq").build())))
                .normal(data("normal",
                        ImmutableFastqConfiguration.builder().read1("normal_read1.fastq").read2("normal_read2.fastq").build()))
                .build()));
        TumorNormalPair pair = pairs.get(0);
        assertThat(pair.tumor().lanes()).containsOnly(ImmutableLane.builder()
                .laneNumber("1")
                .firstOfPairPath("first_tumor_read1.fastq")
                .secondOfPairPath("first_tumor_read2.fastq")
                .build());
        assertThat(pair.reference().lanes()).containsOnly(ImmutableLane.builder()
                .laneNumber("1")
                .firstOfPairPath("normal_read1.fastq")
                .secondOfPairPath("normal_read2.fastq")
                .build());
    }

    public ImmutableRawDataConfiguration data(final String first_tumor, final FastqConfiguration... fastq) {
        return ImmutableRawDataConfiguration.builder().name(first_tumor).addFastq(fastq).build();
    }

    public String stringOf(final int chars) {
        return IntStream.rangeClosed(0, chars - 1).boxed().map(i -> " ").collect(Collectors.joining());
    }
}