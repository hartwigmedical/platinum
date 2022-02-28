package com.hartwig.platinum.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;

import org.junit.Test;

@SuppressWarnings("unchecked")
public class SampleBucketTest {

    @Test
    public void emptyBucketReturnsEmptySamples() {
        Bucket bucket = mock(Bucket.class);
        Page<Blob> page = mock(Page.class);
        when(page.iterateAll()).thenReturn(Collections.emptyList());
        when(bucket.list(Storage.BlobListOption.currentDirectory())).thenReturn(page);
        SampleBucket victim = new SampleBucket(bucket);
        assertThat(victim.apply()).isEmpty();
    }

    @Test
    public void createsSampleConfigurationFromBucketPath() {
        Bucket bucket = mock(Bucket.class);
        when(bucket.getName()).thenReturn("bucket");
        Page<Blob> samplePage = mock(Page.class);
        Blob setDirectory = blob("COLO829/");
        when(samplePage.iterateAll()).thenReturn(List.of(setDirectory));
        when(bucket.list(Storage.BlobListOption.currentDirectory())).thenReturn(samplePage);

        Blob normalDirectory = blob("COLO829/NORMAL/COLO829R");
        Page<Blob> normalDirectoryPage = mock(Page.class);
        when(normalDirectoryPage.iterateAll()).thenReturn(List.of(normalDirectory));
        when(bucket.list(Storage.BlobListOption.prefix("COLO829/NORMAL"), Storage.BlobListOption.currentDirectory())).thenReturn(
                normalDirectoryPage);

        Blob normalFastqR1 = blob("COLO829/NORMAL/COLO829R/COLO829R_R1_001.fastq.gz");
        Blob normalFastqR2 = blob("COLO829/NORMAL/COLO829R/COLO829R_R2_001.fastq.gz");
        Page<Blob> normalFastqPage = mock(Page.class);
        when(normalFastqPage.iterateAll()).thenReturn(List.of(normalFastqR1, normalFastqR2));
        when(bucket.list(Storage.BlobListOption.prefix("COLO829/NORMAL"))).thenReturn(normalFastqPage);

        Blob tumorDirectory = blob("COLO829/NORMAL/COLO829T");
        Page<Blob> tumorDirectoryPage = mock(Page.class);
        when(tumorDirectoryPage.iterateAll()).thenReturn(List.of(tumorDirectory));
        when(bucket.list(Storage.BlobListOption.prefix("COLO829/TUMOR"), Storage.BlobListOption.currentDirectory())).thenReturn(
                tumorDirectoryPage);

        Blob tumorFastqR1 = blob("COLO829/TUMOR/COLO829T/COLO829T_R1_001.fastq.gz");
        Blob tumorFastqR2 = blob("COLO829/TUMOR/COLO829T/COLO829T_R2_001.fastq.gz");
        Page<Blob> tumorFastqPage = mock(Page.class);
        when(tumorFastqPage.iterateAll()).thenReturn(List.of(tumorFastqR1, tumorFastqR2));
        when(bucket.list(Storage.BlobListOption.prefix("COLO829/TUMOR"))).thenReturn(tumorFastqPage);

        SampleBucket victim = new SampleBucket(bucket);
        final List<SampleConfiguration> result = victim.apply();
        assertThat(result).hasSize(1);
        SampleConfiguration sampleConfiguration = result.get(0);
        assertThat(sampleConfiguration.name()).isEqualTo("COLO829");
        assertThat(sampleConfiguration.normal().name()).isEqualTo("COLO829R");
        assertThat(sampleConfiguration.tumors().get(0).name()).isEqualTo("COLO829T");

        assertThat(sampleConfiguration.normal().fastq().get(0).read1()).isEqualTo("bucket/COLO829/NORMAL/COLO829R/COLO829R_R1_001.fastq"
                + ".gz");
        assertThat(sampleConfiguration.normal().fastq().get(0).read2()).isEqualTo("bucket/COLO829/NORMAL/COLO829R/COLO829R_R2_001.fastq"
                + ".gz");
        assertThat(sampleConfiguration.tumors().get(0).fastq().get(0).read1()).isEqualTo("bucket/COLO829/TUMOR/COLO829T/COLO829T_R1_001"
                + ".fastq.gz");
        assertThat(sampleConfiguration.tumors().get(0).fastq().get(0).read2()).isEqualTo("bucket/COLO829/TUMOR/COLO829T/COLO829T_R2_001"
                + ".fastq.gz");
    }

    private Blob blob(final String name) {
        Blob blob = mock(Blob.class);
        when(blob.getName()).thenReturn(name);
        return blob;
    }

}