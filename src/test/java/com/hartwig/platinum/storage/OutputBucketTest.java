package com.hartwig.platinum.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.hartwig.platinum.config.ImmutableOutputConfiguration;
import com.hartwig.platinum.config.OutputConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class OutputBucketTest {

    private static final String RUN_NAME = "test";
    private static final String BUCKET_NAME = "platinum-output-test";
    public static final String LOCATION = "europe-west4";
    public static final ImmutableOutputConfiguration CONFIGURATION = OutputConfiguration.builder().location(LOCATION).build();
    public static final String CMEK_KEY = "/location/of/key";
    private Storage storage;
    private Bucket bucket;
    private OutputBucket victim;

    @Before
    public void setUp() {
        storage = mock(Storage.class);
        bucket = mock(Bucket.class);
        when(bucket.getName()).thenReturn(BUCKET_NAME);
        when(bucket.getLocation()).thenReturn(LOCATION);
        victim = OutputBucket.from(storage);
    }

    @Test
    public void createsNewBucketInSpecifiedLocation() {
        ArgumentCaptor<BucketInfo> bucketInfoArgumentCaptor = ArgumentCaptor.forClass(BucketInfo.class);
        when(storage.create(bucketInfoArgumentCaptor.capture())).thenReturn(bucket);
        String bucketName = victim.findOrCreate(RUN_NAME, CONFIGURATION);
        assertThat(bucketName).isEqualTo(BUCKET_NAME);
        BucketInfo bucketInfo = bucketInfoArgumentCaptor.getValue();
        assertThat(bucketInfo.getLocation()).isEqualTo(LOCATION);
    }

    @Test
    public void skipsCreationIfBucketAlreadyExists() {
        when(storage.get(BUCKET_NAME)).thenReturn(bucket);
        String bucketName = victim.findOrCreate(RUN_NAME, CONFIGURATION);
        verify(storage, never()).create(Mockito.<BlobInfo>any());
        assertThat(bucketName).isEqualTo(BUCKET_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWhenBucketExistsWithDifferentLocation() {
        when(storage.get(BUCKET_NAME)).thenReturn(bucket);
        victim.findOrCreate(RUN_NAME, OutputConfiguration.builder().location("us-east1").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWhenCmekAlreadyOnBucketButNotConfigured() {
        when(bucket.getDefaultKmsKeyName()).thenReturn(CMEK_KEY);
        when(storage.get(BUCKET_NAME)).thenReturn(bucket);
        victim.findOrCreate(RUN_NAME, OutputConfiguration.builder().location(LOCATION).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWhenCmekNotOnBucketButIsConfigured() {
        when(storage.get(BUCKET_NAME)).thenReturn(bucket);
        victim.findOrCreate(RUN_NAME, OutputConfiguration.builder().location(LOCATION).cmek(CMEK_KEY).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWhenCmekOnBucketAndConfiguredDifferently() {
        when(bucket.getDefaultKmsKeyName()).thenReturn(CMEK_KEY);
        when(storage.get(BUCKET_NAME)).thenReturn(bucket);
        victim.findOrCreate(RUN_NAME, OutputConfiguration.builder().location(LOCATION).cmek("/another/key").build());
    }

    @Test
    public void appliesCmekIfSpecifiedInConfig() {
        ArgumentCaptor<BucketInfo> bucketInfoArgumentCaptor = ArgumentCaptor.forClass(BucketInfo.class);
        when(storage.create(bucketInfoArgumentCaptor.capture())).thenReturn(bucket);
        victim.findOrCreate(RUN_NAME, OutputConfiguration.builder().location(LOCATION).cmek(CMEK_KEY).build());
        assertThat(bucketInfoArgumentCaptor.getValue().getDefaultKmsKeyName()).isEqualTo(CMEK_KEY);
    }
}