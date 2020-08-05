package com.hartwig.platinum.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.CreateServiceAccountRequest;
import com.google.api.services.iam.v1.model.ListServiceAccountsResponse;
import com.google.api.services.iam.v1.model.ServiceAccount;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class PipelineServiceAccountTest {

    private static final String SERVICE_ACCOUNT_NAME = "platinum-test";
    private static final String EMAIL = SERVICE_ACCOUNT_NAME + "@service-account.com";
    private static final String PROJECT = "hmf-test";
    private static final String PROJECT_RESOURCE_NAME = "projects/" + PROJECT;
    private static final String RUN_NAME = "test";
    private PipelineServiceAccount victim;
    private ArgumentCaptor<String> projectArgumentCaptor;
    private ArgumentCaptor<CreateServiceAccountRequest> createServiceAccountRequestArgumentCaptor;
    private ListServiceAccountsResponse listServiceAccountsResponse;
    private Iam.Projects.ServiceAccounts serviceAccounts;
    private PipelineIamPolicy iamPolicy;
    private ServiceAccount serviceAccount;

    @Before
    public void setUp() throws Exception {
        final Iam iam = mock(Iam.class);
        Iam.Projects projects = mock(Iam.Projects.class);
        serviceAccounts = mock(Iam.Projects.ServiceAccounts.class);
        Iam.Projects.ServiceAccounts.List listServiceAccounts = mock(Iam.Projects.ServiceAccounts.List.class);
        listServiceAccountsResponse = mock(ListServiceAccountsResponse.class);
        when(iam.projects()).thenReturn(projects);
        when(projects.serviceAccounts()).thenReturn(serviceAccounts);
        when(serviceAccounts.list(PROJECT_RESOURCE_NAME)).thenReturn(listServiceAccounts);
        when(listServiceAccounts.execute()).thenReturn(listServiceAccountsResponse);

        Iam.Projects.ServiceAccounts.Create create = mock(Iam.Projects.ServiceAccounts.Create.class);
        serviceAccount = mock(ServiceAccount.class);
        projectArgumentCaptor = ArgumentCaptor.forClass(String.class);
        createServiceAccountRequestArgumentCaptor = ArgumentCaptor.forClass(CreateServiceAccountRequest.class);
        when(create.execute()).thenReturn(serviceAccount);
        when(serviceAccounts.create(projectArgumentCaptor.capture(),
                createServiceAccountRequestArgumentCaptor.capture())).thenReturn(create);
        when(serviceAccount.getEmail()).thenReturn(EMAIL);

        iamPolicy = mock(PipelineIamPolicy.class);
        victim = new PipelineServiceAccount(iam, iamPolicy);
    }

    @Test
    public void createsServiceAccountIfNotExists() {
        when(listServiceAccountsResponse.getAccounts()).thenReturn(Collections.emptyList());
        String serviceAccountEmail = victim.findOrCreate(PROJECT, RUN_NAME);
        assertThat(serviceAccountEmail).isEqualTo(EMAIL);
        assertThat(projectArgumentCaptor.getValue()).isEqualTo(PROJECT_RESOURCE_NAME);
        assertThat(createServiceAccountRequestArgumentCaptor.getValue().getAccountId()).isEqualTo(SERVICE_ACCOUNT_NAME);
    }

    @Test
    public void skipsCreationWhenServiceAccountExists() throws Exception {
        when(listServiceAccountsResponse.getAccounts()).thenReturn(Collections.singletonList(new ServiceAccount().setEmail(EMAIL)));
        String serviceAccountEmail = victim.findOrCreate(PROJECT, RUN_NAME);
        assertThat(serviceAccountEmail).isEqualTo(EMAIL);
        verify(serviceAccounts, never()).create(any(), any());
    }

    @Test
    public void handlesNullResponseFromServiceAccountList() {
        when(listServiceAccountsResponse.getAccounts()).thenReturn(null);
        String serviceAccountEmail = victim.findOrCreate(PROJECT, RUN_NAME);
        assertThat(serviceAccountEmail).isEqualTo(EMAIL);
    }

    @Test
    public void appliesIamPolicy() {
        victim.findOrCreate(PROJECT, RUN_NAME);
        verify(iamPolicy).apply(serviceAccount);
    }

    @Test
    public void deletesServiceAccount() throws Exception {
        ArgumentCaptor<String> accountNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        when(serviceAccounts.delete(accountNameArgumentCaptor.capture())).thenReturn(mock(Iam.Projects.ServiceAccounts.Delete.class));
        victim.delete(PROJECT, EMAIL);
        assertThat(accountNameArgumentCaptor.getValue()).isEqualTo("/projects/hmf-test/serviceAccounts/" + EMAIL);
    }
}