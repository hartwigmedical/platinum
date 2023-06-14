package com.hartwig.platinum.pdl;

import static java.lang.String.format;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.hartwig.api.RunApi;
import com.hartwig.api.SampleApi;
import com.hartwig.api.SetApi;
import com.hartwig.api.model.Ini;
import com.hartwig.api.model.Run;
import com.hartwig.api.model.SampleSet;
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
    public Map<String, Future<PipelineInput>> apply(final PlatinumConfiguration configuration) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Map<String, Future<PipelineInput>> futures = new HashMap<>();
        for (String biopsy: configuration.sampleIds()) {
            futures.put(biopsy, executorService.submit(() -> {
                SampleSet set = sampleApi.list(null, null, null, null, SampleType.TUMOR, biopsy, null)
                        .stream()
                        .flatMap(sample -> setApi.list(null, sample.getId(), true).stream())
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(format("No set for biopsy [%s]", biopsy)));
                Run researchRun = runApi.list(Status.VALIDATED, Ini.SOMATIC_INI, set.getId(), null, null, null, null, "RESEARCH")
                        .stream()
                        .filter(run -> run.getEndTime() != null)
                        .max(Comparator.comparing(Run::getEndTime))
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(format("No research run for set [%s]", set.getId())));
                return ImmutablePipelineInput.copyOf(pdlGenerator.generate(researchRun.getId())).withOperationalReferences(Optional.empty());
            }));
        }
        return futures;
    }
}