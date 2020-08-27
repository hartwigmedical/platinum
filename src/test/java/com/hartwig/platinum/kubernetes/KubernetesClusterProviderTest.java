package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

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

import org.junit.Before;
import org.junit.Test;

public class KubernetesClusterProviderTest {
    private Container container;
    private Projects projects;
    private Locations locations;
    private Clusters clusters;
    private String project;
    private String region;

    @Before
    public void setup() {
        container = mock(Container.class);
        projects = mock(Projects.class);
        locations = mock(Locations.class);
        clusters = mock(Clusters.class);
        when(container.projects()).thenReturn(projects);
        when(projects.locations()).thenReturn(locations);
        when(locations.clusters()).thenReturn(clusters);

        project = "project";
        region = "region";
    }

    @Test
    public void shouldReturnExistingInstanceIfFound() throws IOException {
        Get foundOperation = mock(Get.class);
        when(clusters.get(anyString())).thenReturn(foundOperation);
        when(foundOperation.execute()).thenReturn(mock(Cluster.class));

        new KubernetesClusterProvider(container).findOrCreate("runName", project, region);
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

        new KubernetesClusterProvider(container).findOrCreate("runName", project, region);
        verify(created).execute();
    }

    @Test
    public void shouldPollUntilCreateClusterOperationHasCompleted() {

    }

    @Test
    public void shouldThrowIfGcloudCredentialFetchFails() {

    }

    @Test
    public void shouldThrowIfKubectlCommandFails() {

    }

}