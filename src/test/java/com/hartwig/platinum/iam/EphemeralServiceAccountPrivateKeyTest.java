package com.hartwig.platinum.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.CreateServiceAccountKeyRequest;
import com.google.api.services.iam.v1.model.ServiceAccountKey;

import org.junit.Before;
import org.junit.Test;

public class EphemeralServiceAccountPrivateKeyTest {

    public static final String PROJECT = "project";
    public static final String EMAIL = "service-account@service-account.com";
    public static final String DATA = "data";
    public static final String NAME = "name";
    private EphemeralServiceAccountPrivateKey victim;
    private Iam.Projects.ServiceAccounts.Keys serviceAccountsKeys;
    private Iam.Projects.ServiceAccounts.Keys.Create serviceAccountKeyCreate;

    @Before
    public void setUp() {
        final Iam iam = mock(Iam.class);
        Iam.Projects projects = mock(Iam.Projects.class);
        Iam.Projects.ServiceAccounts serviceAccounts = mock(Iam.Projects.ServiceAccounts.class);
        serviceAccountsKeys = mock(Iam.Projects.ServiceAccounts.Keys.class);
        serviceAccountKeyCreate = mock(Iam.Projects.ServiceAccounts.Keys.Create.class);
        when(iam.projects()).thenReturn(projects);
        when(projects.serviceAccounts()).thenReturn(serviceAccounts);
        when(serviceAccounts.keys()).thenReturn(serviceAccountsKeys);
        victim = new EphemeralServiceAccountPrivateKey(iam);
    }

    @Test
    public void createsAndReturnsContentAndName() throws Exception {
        ServiceAccountKey serviceAccountKey = new ServiceAccountKey().setName(NAME).setPrivateKeyData(DATA);
        when(serviceAccountsKeys.create(ServiceAccountId.from(PROJECT, EMAIL), new CreateServiceAccountKeyRequest())).thenReturn(
                serviceAccountKeyCreate);
        when(serviceAccountKeyCreate.execute()).thenReturn(serviceAccountKey);
        JsonKey key = victim.create(PROJECT, EMAIL);
         assertThat(key.jsonBase64()).isEqualTo(DATA);
    }
}