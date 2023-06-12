package com.hartwig.platinum.pdl;

import java.util.ArrayList;
import java.util.List;

import com.hartwig.pdl.PipelineInput;
import com.hartwig.pdl.generator.PdlGenerator;
import com.hartwig.platinum.ApiRerun;
import com.hartwig.platinum.config.PlatinumConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertForRerun implements PDLConversion {

    private final PdlGenerator generator;
    private final ApiRerun apiRerun;

    private final Logger LOGGER = LoggerFactory.getLogger(ConvertForRerun.class);

    public ConvertForRerun(final PdlGenerator generator, final ApiRerun apiRerun) {
        this.generator = generator;
        this.apiRerun = apiRerun;
    }

    @Override
    public List<PipelineInput> apply(final PlatinumConfiguration configuration) {
        List<PipelineInput> inputs = new ArrayList<>();
        configuration.sampleIds().parallelStream().forEach(sample -> {
            try {
                inputs.add(generator.generate(apiRerun.create(sample)));
            } catch (Exception e) {
                LOGGER.error("Failed to generate PDL for [{}]", sample, e);
            }
        });
        return inputs;
    }
}