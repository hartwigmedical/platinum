package com.hartwig.platinum;

import static java.lang.String.format;

import com.hartwig.ApiException;
import com.hartwig.api.RunApi;
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
    private final String bucket;
    private final String version;

    public ApiRerun(final RunApi runApi, final SetApi setApi, final String bucket, final String version) {
        this.runApi = runApi;
        this.setApi = setApi;
        this.bucket = bucket;
        this.version = version;
    }

    public long create(final String tumorSampleName) {
        try {
            SampleSet set = setApi.canonical(tumorSampleName, SampleType.TUMOR);
            return getOrCreateRun(tumorSampleName, set);
        } catch (ApiException e) {
            throw new IllegalArgumentException(format("No sets with consent for the database could be found for [%s]", tumorSampleName));
        } catch (IllegalStateException e) {
            LOGGER.error("Failed to get or create run for [{}]", tumorSampleName);
            throw e;
        }
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
