package com.hartwig.platinum.kubernetes.pipeline;

import com.hartwig.pdl.PdlJsonConversion;
import com.hartwig.pdl.PipelineInput;
import com.hartwig.platinum.kubernetes.pipeline.PipelineConfigMapVolume.PipelineConfigMapVolumeBuilder;

import io.fabric8.kubernetes.api.model.Volume;

public class PipelineConfigMapBuilder {

    private final PipelineConfigMapVolumeBuilder pipelineConfigMapVolumeBuilder;
    private final String runName;

    public PipelineConfigMapBuilder(final PipelineConfigMapVolumeBuilder pipelineConfigMapVolumeBuilder,
            final String runName) {
        this.pipelineConfigMapVolumeBuilder = pipelineConfigMapVolumeBuilder;
        this.runName = runName;
    }

    public Volume forSample(String sample, PipelineInput pipelineInput) {
        return pipelineConfigMapVolumeBuilder.of(runName, sample, PdlJsonConversion.getInstance().serialize(pipelineInput)).asKubernetes();
    }
}
