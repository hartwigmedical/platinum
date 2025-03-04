package com.hartwig.platinum.pdl;

import java.util.List;
import java.util.function.Supplier;

import com.hartwig.api.HmfApi;
import com.hartwig.lama.client.DefaultApi;
import com.hartwig.lama.client.LamaClient;
import com.hartwig.pdl.PipelineInput;
import com.hartwig.pdl.generator.PdlGenerator;
import com.hartwig.pdl.generator.sample.Lamav2Api;
import com.hartwig.platinum.ApiRerun;
import com.hartwig.platinum.config.PlatinumConfiguration;

public interface PDLConversion {

    List<Supplier<PipelineInput>> apply(final PlatinumConfiguration configuration);

    static PDLConversion create(final PlatinumConfiguration configuration) {
        return configuration.hmfConfiguration().map(hmfConfiguration -> {
            HmfApi api = HmfApi.create(hmfConfiguration.apiUrl());

            Lamav2Api lamav2Api = new Lamav2Api(getLamaClient(configuration));
            PdlGenerator pdlGenerator = PdlGenerator.forResearch(api, lamav2Api);
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

    private static DefaultApi getLamaClient(final PlatinumConfiguration configuration) {
        return configuration.lamaUrl().map(LamaClient::to).orElse(null);
    }

    private static String extractVersion(final String imageName) {
        return imageName.split(":")[1].split("-")[0];
    }
}
