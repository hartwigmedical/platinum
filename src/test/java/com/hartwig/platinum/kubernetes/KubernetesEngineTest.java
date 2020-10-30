package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import static org.junit.Assert.fail;
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
import com.google.api.services.container.v1beta1.model.Operation;
import com.hartwig.platinum.GcpConfiguration;
import com.hartwig.platinum.ImmutableGcpConfiguration;
import com.hartwig.platinum.iam.JsonKey;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

public class KubernetesEngineTest {
    private Container container;
    private Locations locations;
    private Clusters clusters;
    private ProcessRunner processRunner;
    private String project;
    private String region;
    private ImmutableGcpConfiguration configuration;

    @Before
    public void setup() {
        final Projects projects = mock(Projects.class);
        processRunner = mock(ProcessRunner.class);
        container = mock(Container.class);
        locations = mock(Locations.class);
        clusters = mock(Clusters.class);
        when(container.projects()).thenReturn(projects);
        when(projects.locations()).thenReturn(locations);
        when(locations.clusters()).thenReturn(clusters);
        when(processRunner.execute(anyList())).thenReturn(true);

        project = "project";
        region = "region";
        configuration = GcpConfiguration.builder().project(project).region(region).privateCluster(false).build();
    }

    @Test
    public void shouldReturnExistingInstanceIfFound() throws IOException {
        mocksForClusterExists();

        new KubernetesEngine(container, processRunner).findOrCreate("runName",
                configuration,
                JsonKey.of("id", "json"),
                "bucket",
                "service_account");
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
        when(clusters.create(eq(format("projects/%s/locations/%s", project, region)), any())).thenReturn(created);
        when(created.execute()).thenReturn(executedCreate);
        when(executedCreate.getName()).thenReturn("created");

        Operations operations = mock(Operations.class);
        Operations.Get operationsGet = mock(Operations.Get.class);
        Operation executedOperationsGet = mock(Operation.class);
        when(locations.operations()).thenReturn(operations);
        when(operations.get(anyString())).thenReturn(operationsGet);
        when(operationsGet.execute()).thenReturn(executedOperationsGet);
        when(executedOperationsGet.getStatus()).thenReturn("DONE");

        new KubernetesEngine(container, processRunner).findOrCreate("runName",
                configuration,
                JsonKey.of("id", "json"),
                "bucket",
                "service_account");
        verify(created).execute();
    }

    @Test
    public void shouldPollUntilCreateClusterOperationHasCompleted() throws Exception {
        Get foundOperation = mock(Get.class);
        Create created = mock(Create.class);
        Operation executedCreate = mock(Operation.class);

        when(clusters.get(anyString())).thenReturn(foundOperation);
        when(foundOperation.execute()).thenThrow(GoogleJsonResponseException.class);
        when(clusters.create(eq(format("projects/%s/locations/%s", project, region)), any())).thenReturn(created);
        when(created.execute()).thenReturn(executedCreate);
        when(executedCreate.getName()).thenReturn("created");

        Operations operations = mock(Operations.class);
        Operations.Get operationsGet = mock(Operations.Get.class);
        Operation executedOperationsGet = mock(Operation.class);
        when(locations.operations()).thenReturn(operations);
        when(operations.get(anyString())).thenReturn(operationsGet);
        when(operationsGet.execute()).thenReturn(executedOperationsGet);
        when(executedOperationsGet.getStatus()).thenReturn(null).thenReturn("RUNNING").thenReturn("DONE");

        new KubernetesEngine(container, processRunner).findOrCreate("runName",
                configuration,
                JsonKey.of("id", "json"),
                "bucket",
                "service_account");
        verify(executedOperationsGet, times(3)).getStatus();
    }

    @Test
    public void shouldThrowIfGcloudCredentialFetchFails() {
        mocksForClusterExists();
        when(processRunner.execute(argThat(isListStartingWith("gcloud")))).thenReturn(false);
        try {
            new KubernetesEngine(container, processRunner).findOrCreate("runName",
                    configuration,
                    JsonKey.of("id", "json"),
                    "bucket",
                    "service_account");
            fail("Expected an exception");
        } catch (RuntimeException e) {
            // OK
        }
    }

    @Test
    public void shouldThrowIfKubectlCommandFails() {
        mocksForClusterExists();
        when(processRunner.execute(anyList())).thenReturn(true).thenReturn(false);
        try {
            new KubernetesEngine(container, processRunner).findOrCreate("runName",
                    configuration,
                    JsonKey.of("id", "json"),
                    "bucket",
                    "service_account");
            fail("Expected an exception");
        } catch (RuntimeException e) {
            // OK
        }
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

    private ArgumentMatcher<List<String>> isListStartingWith(String startingElement) {
        return argument -> argument.get(0).equals(startingElement);
    }
}