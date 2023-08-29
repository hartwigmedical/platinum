package com.hartwig.platinum.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.Policy;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.config.ServiceAccountConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class OutputBucketTest {

    private static final String RUN_NAME = "test";
    private static final String BUCKET_NAME = "platinum-output-test";
    public static final String REGION = "europe-west4";
    private static final PlatinumConfiguration CONFIGURATION =
            PlatinumConfiguration.builder().gcp(GcpConfiguration.builder().build())
                    .serviceAccount(ServiceAccountConfiguration.builder().kubernetesServiceAccount("ksa").gcpEmailAddress("gcp").build())
                    .build();
    private static final String CMEK_KEY = "/location/of/key";
    private Storage storage;
    private Bucket bucket;
    private OutputBucket victim;

    @Before
    public void setUp() {
        storage = mock(Storage.class);
        bucket = mock(Bucket.class);
        when(bucket.getName()).thenReturn(BUCKET_NAME);
        when(bucket.getLocation()).thenReturn(REGION);
        when(storage.getIamPolicy(BUCKET_NAME)).thenReturn(Policy.newBuilder().build());
        victim = OutputBucket.from(storage);
    }

    @Test
    public void createsNewBucketInSpecifiedLocation() {
        ArgumentCaptor<BucketInfo> bucketInfoArgumentCaptor = ArgumentCaptor.forClass(BucketInfo.class);
        when(storage.create(bucketInfoArgumentCaptor.capture())).thenReturn(bucket);
        String bucketName = victim.findOrCreate(RUN_NAME, REGION, CONFIGURATION);
        assertThat(bucketName).isEqualTo(BUCKET_NAME);
        BucketInfo bucketInfo = bucketInfoArgumentCaptor.getValue();
        assertThat(bucketInfo.getLocation()).isEqualTo(REGION);
    }

    @Test
    public void usingExistingBucketWhenFound() {
        ArgumentCaptor<BucketInfo> bucketInfoArgumentCaptor = ArgumentCaptor.forClass(BucketInfo.class);
        when(storage.create(bucketInfoArgumentCaptor.capture())).thenReturn(bucket);
        when(storage.get(BUCKET_NAME)).thenReturn(bucket);
        String bucketName = victim.findOrCreate(RUN_NAME, REGION, CONFIGURATION);
        verify(storage, never()).create(Mockito.<BucketInfo>any());
        assertThat(bucketName).isEqualTo(BUCKET_NAME);
    }

    @Test
    public void usesBucketFromConfigurationIfPresent() {
        ArgumentCaptor<BucketInfo> bucketInfoArgumentCaptor = ArgumentCaptor.forClass(BucketInfo.class);
        when(storage.create(bucketInfoArgumentCaptor.capture())).thenReturn(bucket);
        String configuredBucket = "configuredBucket";
        when(storage.getIamPolicy(configuredBucket)).thenReturn(Policy.newBuilder().build());
        String bucketName = victim.findOrCreate(RUN_NAME,
                REGION,
                PlatinumConfiguration.builder().from(CONFIGURATION).outputBucket(configuredBucket).build());
        assertThat(bucketName).isEqualTo(BUCKET_NAME);
        BucketInfo bucketInfo = bucketInfoArgumentCaptor.getValue();
        assertThat(bucketInfo.getName()).isEqualTo(configuredBucket);
    }

    @Test
    public void appliesCmekIfSpecifiedInConfig() {
        ArgumentCaptor<BucketInfo> bucketInfoArgumentCaptor = ArgumentCaptor.forClass(BucketInfo.class);
        when(storage.create(bucketInfoArgumentCaptor.capture())).thenReturn(bucket);
        victim.findOrCreate(RUN_NAME, REGION, PlatinumConfiguration.builder().from(CONFIGURATION).cmek(CMEK_KEY).build());
        assertThat(bucketInfoArgumentCaptor.getValue().getDefaultKmsKeyName()).isEqualTo(CMEK_KEY);
    }
}