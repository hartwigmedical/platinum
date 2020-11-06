package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.container.v1beta1.Container;
import com.google.api.services.container.v1beta1.Container.Projects;
import com.google.api.services.container.v1beta1.Container.Projects.Locations;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters.Create;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters.Get;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Operations;
import com.google.api.services.container.v1beta1.model.Cluster;
import com.google.api.services.container.v1beta1.model.CreateClusterRequest;
import com.google.api.services.container.v1beta1.model.Operation;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.ImmutablePlatinumConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.iam.JsonKey;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

public class KubernetesEngineTest {
    private static final String PROJECT = "project";
    private static final String REGION = "region";
    private static final ImmutablePlatinumConfiguration CONFIGURATION =
            PlatinumConfiguration.builder().gcp(GcpConfiguration.builder().project(PROJECT).region(REGION).build()).build();
    public static final String RUN_NAME = "runName";
    public static final String BUCKET = "bucket";
    public static final String SERVICE_ACCOUNT = "service_account";
    public static final JsonKey JSON_KEY = JsonKey.of("id", "json");
    private Locations locations;
    private Clusters clusters;
    private ProcessRunner processRunner;
    private KubernetesEngine victim;

    @Before
    public void setup() {
        final Projects projects = mock(Projects.class);
        processRunner = mock(ProcessRunner.class);
        final Container container = mock(Container.class);
        locations = mock(Locations.class);
        clusters = mock(Clusters.class);
        when(container.projects()).thenReturn(projects);
        when(projects.locations()).thenReturn(locations);
        when(locations.clusters()).thenReturn(clusters);
        when(processRunner.execute(anyList())).thenReturn(true);
        victim = new KubernetesEngine(container, processRunner);
    }

    @Test
    public void shouldReturnExistingInstanceIfFound() throws IOException {
        mocksForClusterExists();
        victim.findOrCreate(RUN_NAME, CONFIGURATION, JSON_KEY, BUCKET, SERVICE_ACCOUNT);
        verify(clusters).get(anyString());
        verify(clusters, never()).create(any(), any());
    }

    @Test
    public void shouldCreateAndReturnInstanceWhenNoneExists() throws IOException {
        Get foundOperation = mock(Get.class);
        Create created = mock(Create.class);
        Operation executedCreate = mock(Operation.class);

        when(clusters.get(anyString())).thenReturn(foundOperation);
        when(foundOperation.execute()).thenThrow(GoogleJsonResponseException.class);
        when(clusters.create(eq(format("projects/%s/locations/%s", PROJECT, REGION)), any())).thenReturn(created);
        when(created.execute()).thenReturn(executedCreate);
        when(executedCreate.getName()).thenReturn("created");

        mockForClusterCreation();

        victim.findOrCreate(RUN_NAME, CONFIGURATION, JSON_KEY, BUCKET, SERVICE_ACCOUNT);
        verify(created).execute();
    }

    @Test
    public void shouldPollUntilCreateClusterOperationHasCompleted() throws Exception {
        Get foundOperation = mock(Get.class);
        Create created = mock(Create.class);
        Operation executedCreate = mock(Operation.class);

        when(clusters.get(anyString())).thenReturn(foundOperation);
        when(foundOperation.execute()).thenThrow(GoogleJsonResponseException.class);
        when(clusters.create(eq(format("projects/%s/locations/%s", PROJECT, REGION)), any())).thenReturn(created);
        when(created.execute()).thenReturn(executedCreate);
        when(executedCreate.getName()).thenReturn("created");

        Operations operations = mock(Operations.class);
        Operations.Get operationsGet = mock(Operations.Get.class);
        Operation executedOperationsGet = mock(Operation.class);
        when(locations.operations()).thenReturn(operations);
        when(operations.get(anyString())).thenReturn(operationsGet);
        when(operationsGet.execute()).thenReturn(executedOperationsGet);
        when(executedOperationsGet.getStatus()).thenReturn(null).thenReturn("RUNNING").thenReturn("DONE");

        victim.findOrCreate(RUN_NAME, CONFIGURATION, JSON_KEY, BUCKET, SERVICE_ACCOUNT);
        //noinspection ResultOfMethodCallIgnored
        verify(executedOperationsGet, times(3)).getStatus();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowIfGcloudCredentialFetchFails() {
        mocksForClusterExists();
        when(processRunner.execute(argThat(startsWithGcloud()))).thenReturn(false);
        victim.findOrCreate(RUN_NAME, CONFIGURATION, JSON_KEY, BUCKET, SERVICE_ACCOUNT);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowIfKubectlCommandFails() {
        mocksForClusterExists();
        when(processRunner.execute(anyList())).thenReturn(true).thenReturn(false);
        victim.findOrCreate(RUN_NAME, CONFIGURATION, JSON_KEY, BUCKET, SERVICE_ACCOUNT);
    }

    @Test
    public void usesConfiguredClusterName() throws Exception {

        Get foundOperation = mock(Get.class);
        Create created = mock(Create.class);
        Operation executedCreate = mock(Operation.class);

        when(clusters.get(anyString())).thenReturn(foundOperation);
        when(clusters.create(eq(format("projects/%s/locations/%s", PROJECT, REGION)), any())).thenReturn(created);
        when(created.execute()).thenReturn(executedCreate);
        when(executedCreate.getName()).thenReturn("created");

        when(clusters.get(anyString())).thenReturn(foundOperation);
        when(foundOperation.execute()).thenThrow(GoogleJsonResponseException.class);
        ArgumentCaptor<CreateClusterRequest> createRequest = ArgumentCaptor.forClass(CreateClusterRequest.class);
        when(clusters.create(eq(format("projects/%s/locations/%s", PROJECT, REGION)),
                createRequest.capture())).thenReturn(created);
        when(created.execute()).thenReturn(executedCreate);
        when(executedCreate.getName()).thenReturn("created");
        mockForClusterCreation();
        victim.findOrCreate(RUN_NAME,
                PlatinumConfiguration.builder().from(CONFIGURATION).cluster("cluster").build(),
                JSON_KEY,
                BUCKET,
                SERVICE_ACCOUNT);

        assertThat(createRequest.getValue().getCluster().getName()).isEqualTo("cluster");
    }

    public void mockForClusterCreation() throws IOException {
        Operations operations = mock(Operations.class);
        Operations.Get operationsGet = mock(Operations.Get.class);
        Operation executedOperationsGet = mock(Operation.class);
        when(locations.operations()).thenReturn(operations);
        when(operations.get(anyString())).thenReturn(operationsGet);
        when(operationsGet.execute()).thenReturn(executedOperationsGet);
        when(executedOperationsGet.getStatus()).thenReturn("DONE");
    }

    private void mocksForClusterExists() {
        try {
            Get foundOperation = mock(Get.class);
            when(clusters.get(anyString())).thenReturn(foundOperation);
            when(foundOperation.execute()).thenReturn(mock(Cluster.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ArgumentMatcher<List<String>> startsWithGcloud() {
        return argument -> argument.get(0).equals("gcloud");
    }
}