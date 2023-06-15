package com.hartwig.platinum.pdl;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.hartwig.pdl.PipelineInput;
import com.hartwig.pdl.generator.PdlGenerator;
import com.hartwig.platinum.ApiRerun;
import com.hartwig.platinum.config.PlatinumConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertForRerun implements PDLConversion {

    private final PdlGenerator generator;
    private final ApiRerun apiRerun;

    public ConvertForRerun(final PdlGenerator generator, final ApiRerun apiRerun) {
        this.generator = generator;
        this.apiRerun = apiRerun;
    }

    @Override
    public List<Supplier<PipelineInput>> apply(final PlatinumConfiguration configuration) {
        return configuration.sampleIds()
                .stream()
                .map(sampleId -> (Supplier<PipelineInput>) () -> generator.generate(apiRerun.create(sampleId)))
                .collect(Collectors.toList());
    }
}