package com.hartwig.platinum;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.hartwig.api.RunApi;
import com.hartwig.api.SampleApi;
import com.hartwig.api.SetApi;
import com.hartwig.api.helpers.OnlyOne;
import com.hartwig.api.model.CreateRun;
import com.hartwig.api.model.Ini;
import com.hartwig.api.model.Run;
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

    public long create(final String tumorSampleName) {
        List<SampleSet> sets = setApi.list(null,
                sampleApi.list(null, null, null, null, SampleType.TUMOR, tumorSampleName, null)
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(format("Cannot find tumor sample [%s]", tumorSampleName)))
                        .getId(),
                true);
        if (sets.isEmpty()) {
            throw new IllegalArgumentException(format("No sets with consent for the database could be found for [%s]", tumorSampleName));
        }
        return getOrCreateRun(tumorSampleName, sets.size() == 1 ? sets.get(0) : resolveActiveSet(sets, tumorSampleName));
    }

    private SampleSet resolveActiveSet(List<SampleSet> sets, String tumorSampleName) {
        List<SampleSet> setsWithNonInvalidatedRuns = new ArrayList<>();
        for (SampleSet set : sets) {
            List<Run> runs = runApi.list(null, null, set.getId(), null, null, null, null, null);
            if (runs.stream().anyMatch(r -> r.getStatus() != Status.INVALIDATED)) {
                setsWithNonInvalidatedRuns.add(set);
            }
        }
        if (setsWithNonInvalidatedRuns.isEmpty()) {
            throw new IllegalStateException(format("Multiple sets found for tumor sample [%s] but none have non-invalidated runs attached",
                    tumorSampleName));
        } else if (setsWithNonInvalidatedRuns.size() > 1) {
            throw new IllegalStateException(format("Multiple sets with non-invalidated runs found for tumor sample [%s]: %s",
                    tumorSampleName,
                    setsWithNonInvalidatedRuns.stream().map(s -> s.getId().toString()).collect(Collectors.joining(","))));
        }
        return setsWithNonInvalidatedRuns.get(0);
    }

    /**
     * @return Run ID, never null but the openAPI generated classes are limited.
     */
    private Long getOrCreateRun(final String tumorSampleName, final SampleSet sampleSet) {
        return OnlyOne.ofNullable(runApi.list(null, Ini.RERUN_INI, sampleSet.getId(), version, version, null, null, null), Run.class)
                .filter(r -> !r.getStatus().equals(Status.INVALIDATED))
                .map(r -> {
                    LOGGER.info("Using existing run for sample [{}] id [{}]", tumorSampleName, r.getId());
                    return r.getId();
                })
                .orElseGet(() -> {
                    final Long id = runApi.create(new CreateRun().bucket(bucket)
                            .cluster("gcp")
                            .context("RESEARCH")
                            .ini(Ini.RERUN_INI)
                            .version(version)
                            .status(Status.PENDING)
                            .isHidden(true)
                            .setId(sampleSet.getId())).getId();
                    LOGGER.info("Created API run for sample [{}] id [{}]", tumorSampleName, id);
                    return id;
                });
    }
}
