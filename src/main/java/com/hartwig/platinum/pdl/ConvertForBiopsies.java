package com.hartwig.platinum.pdl;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.hartwig.api.RunApi;
import com.hartwig.api.SampleApi;
import com.hartwig.api.SetApi;
import com.hartwig.api.model.Ini;
import com.hartwig.api.model.Run;
import com.hartwig.api.model.SampleType;
import com.hartwig.api.model.Status;
import com.hartwig.pdl.ImmutablePipelineInput;
import com.hartwig.pdl.PipelineInput;
import com.hartwig.pdl.generator.PdlGenerator;
import com.hartwig.platinum.config.PlatinumConfiguration;

public class ConvertForBiopsies implements PDLConversion {

    private final RunApi runApi;
    private final SampleApi sampleApi;
    private final SetApi setApi;
    private final PdlGenerator pdlGenerator;

    public ConvertForBiopsies(final RunApi runApi, final SampleApi sampleApi, final SetApi setApi, final PdlGenerator pdlGenerator) {
        this.runApi = runApi;
        this.sampleApi = sampleApi;
        this.setApi = setApi;
        this.pdlGenerator = pdlGenerator;
    }

    @Override
    public List<Supplier<PipelineInput>> apply(final PlatinumConfiguration configuration) {
        return configuration.sampleIds()
                .stream()
                .flatMap(biopsy -> sampleApi.list(null, null, null, null, SampleType.TUMOR, biopsy, null).stream())
                .flatMap(sample -> setApi.list(null, sample.getId(), true).stream())
                .flatMap(set -> runApi.list(Status.VALIDATED, Ini.SOMATIC_INI, set.getId(), null, null, null, null, "RESEARCH")
                        .stream()
                        .filter(run -> run.getEndTime() != null)
                        .max(Comparator.comparing(Run::getEndTime))
                        .map(run -> (Supplier<PipelineInput>) () -> ImmutablePipelineInput.copyOf(pdlGenerator.generate(run.getId()))
                                .withOperationalReferences(Optional.empty()))
                        .stream())
                .collect(Collectors.toList());
    }
}