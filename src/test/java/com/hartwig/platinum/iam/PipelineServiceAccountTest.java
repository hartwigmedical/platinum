package com.hartwig.platinum.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import java.util.Collections;

import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.CreateServiceAccountRequest;
import com.google.api.services.iam.v1.model.ListServiceAccountsResponse;
import com.google.api.services.iam.v1.model.ServiceAccount;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class PipelineServiceAccountTest {

    private static final String SERVICE_ACCOUNT_ID = "platinum-test";
    private static final String EMAIL = "@service-account.com";
    private static final String PROJECT = "hmf-test";
    private static final String PROJECT_RESOURCE_NAME = "projects/" + PROJECT;
    private static final String RUN_NAME = "test";

    @Test
    public void createsServiceAccountIfNotExists() throws Exception {
        Iam iam = mock(Iam.class);
        Iam.Projects projects = mock(Iam.Projects.class);
        Iam.Projects.ServiceAccounts serviceAccounts = mock(Iam.Projects.ServiceAccounts.class);
        Iam.Projects.ServiceAccounts.List listServiceAccounts = mock(Iam.Projects.ServiceAccounts.List.class);
        ListServiceAccountsResponse listServiceAccountsResponse = mock(ListServiceAccountsResponse.class);
        when(iam.projects()).thenReturn(projects);
        when(projects.serviceAccounts()).thenReturn(serviceAccounts);
        when(serviceAccounts.list(PROJECT_RESOURCE_NAME)).thenReturn(listServiceAccounts);
        when(listServiceAccounts.execute()).thenReturn(listServiceAccountsResponse);
        when(listServiceAccountsResponse.getAccounts()).thenReturn(Collections.emptyList());

        Iam.Projects.ServiceAccounts.Create create = mock(Iam.Projects.ServiceAccounts.Create.class);
        ServiceAccount serviceAccount = mock(ServiceAccount.class);
        ArgumentCaptor<String> projectArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CreateServiceAccountRequest> createServiceAccountRequestArgumentCaptor =
                ArgumentCaptor.forClass(CreateServiceAccountRequest.class);
        when(create.execute()).thenReturn(serviceAccount);
        when(serviceAccounts.create(projectArgumentCaptor.capture(),
                createServiceAccountRequestArgumentCaptor.capture())).thenReturn(create);
        when(serviceAccount.getEmail()).thenReturn(EMAIL);

        PipelineServiceAccount victim = new PipelineServiceAccount(iam);
        String serviceAccountEmail = victim.findOrCreate(PROJECT, RUN_NAME);
        assertThat(serviceAccountEmail).isEqualTo(EMAIL);
        assertThat(projectArgumentCaptor.getValue()).isEqualTo(PROJECT_RESOURCE_NAME);
        assertThat(createServiceAccountRequestArgumentCaptor.getValue().getAccountId()).isEqualTo(SERVICE_ACCOUNT_ID);
    }

}