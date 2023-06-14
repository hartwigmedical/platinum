package com.hartwig.platinum.pdl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    public Map<String, Future<PipelineInput>> apply(final PlatinumConfiguration configuration) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Map<String, Future<PipelineInput>> inputs = new HashMap<>();
        configuration.sampleIds().parallelStream().forEach(sample -> {
            try {
                inputs.put(sample, executorService.submit(() -> generator.generate(apiRerun.create(sample))));
            } catch (Exception e) {
                LOGGER.error("Failed to generate PDL for [{}]", sample, e);
            }
        });
        return inputs;
    }
}