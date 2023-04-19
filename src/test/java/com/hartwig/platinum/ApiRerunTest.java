package com.hartwig.platinum;

import static java.util.Collections.emptyList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import com.hartwig.api.RunApi;
import com.hartwig.api.SampleApi;
import com.hartwig.api.SetApi;
import com.hartwig.api.model.CreateRun;
import com.hartwig.api.model.Ini;
import com.hartwig.api.model.Run;
import com.hartwig.api.model.RunCreated;
import com.hartwig.api.model.Sample;
import com.hartwig.api.model.SampleSet;
import com.hartwig.api.model.SampleType;
import com.hartwig.api.model.Status;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ApiRerunTest {
    private RunApi runs;
    private SetApi sets;
    private SampleApi sampleApi;
    private String bucket = "bucket";
    private String version = "notarealversion";
    private String biopsy = "sample";
    private Long sampleId = 1L;
    private Long sampleSetId = 2L;
    private Run validatedRun = new Run().status(Status.VALIDATED);
    private Run existingReRun = new Run().status(Status.PENDING).id(3L);
    private List<Sample> samples;
    private SampleSet sampleSet;

    @Before
    public void setup() {
        runs = mock(RunApi.class);
        sets = mock(SetApi.class);
        sampleApi = mock(SampleApi.class);

        samples = List.of(new Sample().name(biopsy).id(sampleId));
        sampleSet = new SampleSet().id(sampleSetId);

        when(sampleApi.list(null, null, null, null, SampleType.TUMOR, biopsy, null)).thenReturn(samples);
        when(runs.list(null, null, sampleSet.getId(), null, null, null, null, null)).thenReturn(List.of(validatedRun));
    }

    @Test
    public void shouldReturnIdOfExistingRunIfItIsNotInvalidated() {
        when(sets.list(null, samples.get(0).getId(), true)).thenReturn(List.of(sampleSet));
        when(runs.list(null, Ini.RERUN_INI, sampleSet.getId(), version, version, null, null, null)).thenReturn(List.of(existingReRun));

        assertThat(new ApiRerun(runs, sets, sampleApi, bucket, version).create(biopsy)).isEqualTo(3L);
        verify(runs, never()).create(any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentIfNoSamplesMatchGivenId() {
        when(sampleApi.list(null, null, null, null, SampleType.TUMOR, biopsy, null)).thenReturn(emptyList());
        assertThat(new ApiRerun(runs, sets, sampleApi, bucket, version).create(biopsy)).isNull();
        verify(runs, never()).create(any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentIfNoSampleSetExistsForSample() {
        when(sets.list(null, samples.get(0).getId(), true)).thenReturn(Collections.emptyList());
        assertThat(new ApiRerun(runs, sets, sampleApi, bucket, version).create(biopsy)).isNull();
        verify(runs, never()).create(any());
    }

    @Test
    public void shouldCreateRunForSampleIfNoneExists() {
        when(runs.list(null, Ini.RERUN_INI, sampleSet.getId(), version, version, null, null, null)).thenReturn(emptyList());
        when(sets.list(null, samples.get(0).getId(), true)).thenReturn(List.of(sampleSet));
        ArgumentCaptor<CreateRun> createRunCaptor = ArgumentCaptor.forClass(CreateRun.class);
        when(runs.create(createRunCaptor.capture())).thenReturn(new RunCreated().id(3L));

        assertThat(new ApiRerun(runs, sets, sampleApi, bucket, version).create(biopsy)).isNotNull();
        CreateRun createRun = createRunCaptor.getValue();
        assertThat(createRun).isNotNull();
        assertThat(createRun.getCluster()).isEqualTo("gcp");
        assertThat(createRun.getBucket()).isEqualTo(bucket);
        assertThat(createRun.getIni()).isEqualTo(Ini.RERUN_INI);
        assertThat(createRun.getSetId()).isEqualTo(sampleSetId);
        assertThat(createRun.getVersion()).isEqualTo(version);
        assertThat(createRun.getContext()).isEqualTo("RESEARCH");
        assertThat(createRun.getStatus()).isEqualTo(Status.PENDING);
        assertThat(createRun.getIsHidden()).isEqualTo(true);
    }
}