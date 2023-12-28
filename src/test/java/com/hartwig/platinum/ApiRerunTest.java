package com.hartwig.platinum;

import static java.util.Collections.emptyList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.hartwig.ApiException;
import com.hartwig.api.RunApi;
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
    private final String bucket = "bucket";
    private final String version = "notarealversion";
    private final String biopsy = "sample";
    private final Long sampleSetId = 2L;
    private final Run validatedRun = new Run().status(Status.VALIDATED);
    private final Run existingReRun = new Run().status(Status.PENDING).id(3L);
    private RunApi runs;
    private SetApi sets;
    private SampleSet sampleSet;

    @Before
    public void setup() {
        runs = mock(RunApi.class);
        sets = mock(SetApi.class);

        Long sampleId = 1L;
        List<Sample> samples = List.of(new Sample().name(biopsy).id(sampleId));
        sampleSet = new SampleSet().id(sampleSetId);

        when(runs.callList(null, null, sampleSet.getId(), null, null, null, null, null, null)).thenReturn(List.of(validatedRun));
        when(sets.canonical(biopsy, SampleType.TUMOR)).thenReturn(sampleSet);
    }

    @Test
    public void shouldReturnIdOfExistingRunIfItIsNotInvalidated() {
        when(runs.callList(null, Ini.RERUN_INI, sampleSet.getId(), version, version, null, null, null, null)).thenReturn(List.of(existingReRun));
        assertThat(new ApiRerun(runs, sets, bucket, version).create(biopsy)).isEqualTo(3L);
        verify(runs, never()).create(any());
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfNoCanonicalSetExistsForSample() {
        when(sets.canonical(any(), any())).thenThrow(new ApiException());
        assertThatThrownBy(() -> new ApiRerun(runs, sets, bucket, version).create(biopsy)).isInstanceOf(IllegalArgumentException.class);
        verify(runs, never()).create(any());
    }

    @Test
    public void shouldCreateRunForSampleIfNoneExists() {
        when(runs.callList(null, Ini.RERUN_INI, sampleSet.getId(), version, version, null, null, null, null)).thenReturn(emptyList());
        ArgumentCaptor<CreateRun> createRunCaptor = ArgumentCaptor.forClass(CreateRun.class);
        when(runs.create(createRunCaptor.capture())).thenReturn(new RunCreated().id(3L));

        assertThat(new ApiRerun(runs, sets, bucket, version).create(biopsy)).isNotNull();
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