package com.hartwig.platinum.pdl;

import java.util.List;

import com.hartwig.api.HmfApi;
import com.hartwig.pdl.PipelineInput;
import com.hartwig.pdl.generator.PdlGenerator;
import com.hartwig.platinum.ApiRerun;
import com.hartwig.platinum.config.PlatinumConfiguration;

public interface PDLConversion {

    List<PipelineInput> apply(final PlatinumConfiguration configuration);

    static PDLConversion create(final PlatinumConfiguration configuration) {
        return configuration.hmfConfiguration().map(hmfConfiguration -> {
            HmfApi api = HmfApi.create(hmfConfiguration.apiUrl());
            PdlGenerator pdlGenerator = PdlGenerator.forResearch(api);
            if (hmfConfiguration.isRerun()) {
                return new ConvertForRerun(pdlGenerator,
                        new ApiRerun(api.runs(),
                                api.sets(),
                                configuration.outputBucket()
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                "Cannot execute a rerun without a configured outputBucket in the platinum configuration")),
                                extractVersion(configuration.image())));
            } else {
                return new ConvertForBiopsies(api.runs(), api.samples(), api.sets(), pdlGenerator);
            }
        }).orElse(new ConvertFromGCSPaths());
    }

    private static String extractVersion(final String imageName) {
        return imageName.split(":")[1].split("-")[0];
    }
}
