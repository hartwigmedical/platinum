package com.hartwig.platinum;

import java.util.List;
import java.util.Optional;

import com.hartwig.api.RunApi;
import com.hartwig.api.SampleApi;
import com.hartwig.api.SetApi;
import com.hartwig.api.helpers.OnlyOne;
import com.hartwig.api.model.CreateRun;
import com.hartwig.api.model.Ini;
import com.hartwig.api.model.Run;
import com.hartwig.api.model.Sample;
import com.hartwig.api.model.SampleSet;
import com.hartwig.api.model.SampleType;
import com.hartwig.api.model.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiRerun {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRerun.class);

    private final RunApi runApi;
    private final SetApi setApi;
    private final SampleApi sampleApi;
    private final String bucket;
    private final String version;

    public ApiRerun(final RunApi runApi, final SetApi setApi, final SampleApi sampleApi, final String bucket, final String version) {
        this.runApi = runApi;
        this.setApi = setApi;
        this.sampleApi = sampleApi;
        this.bucket = bucket;
        this.version = version;
    }

    Long create(final String sampleId) {
        try {
            List<Sample> samples = sampleApi.list(null, null, null, null, SampleType.TUMOR, sampleId);
            for (Sample sample : samples) {
                Optional<SampleSet> maybeSampleSet = OnlyOne.ofNullable(setApi.list(null, sample.getId(), true), SampleSet.class);
                if (maybeSampleSet.isPresent()) {
                    SampleSet sampleSet = maybeSampleSet.get();
                    if (runApi.list(null, null, sampleSet.getId(), null, null, null, null, null)
                            .stream()
                            .anyMatch(r1 -> r1.getStatus().equals(Status.VALIDATED))) {
                        return OnlyOne.ofNullable(runApi.list(null, Ini.RERUN_INI, sampleSet.getId(), version, version, null, null, null),
                                Run.class).filter(r -> !r.getStatus().equals(Status.INVALIDATED)).map(r -> {
                            LOGGER.info("Using existing run for sample [{}] id [{}]", sampleId, r.getId());
                            return r.getId();
                        }).orElseGet(() -> {
                            final Long id = runApi.create(new CreateRun().bucket(bucket)
                                    .cluster("gcp")
                                    .context("RESEARCH")
                                    .ini(Ini.RERUN_INI)
                                    .version(version)
                                    .status(Status.PENDING)
                                    .isHidden(true)
                                    .setId(sampleSet.getId())).getId();
                            LOGGER.info("Created API run for sample [{}] id [{}]", sampleId, id);
                            return id;
                        });
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to create run for [{}] reason [{}]", sampleId, e.getMessage());
            return null;
        }
        return null;
    }
}
