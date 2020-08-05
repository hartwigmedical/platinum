package com.hartwig.platinum.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.Binding;
import com.google.api.services.cloudresourcemanager.model.GetIamPolicyRequest;
import com.google.api.services.cloudresourcemanager.model.Policy;
import com.google.api.services.cloudresourcemanager.model.SetIamPolicyRequest;
import com.google.api.services.iam.v1.model.ServiceAccount;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class PipelineIamPolicyTest {

    private static final String PROJECT_ID = "projects/test";
    private static final String EMAIL = "platinum-test@service-account.com";
    private static final String IDENTITY = "serviceAccount:" + EMAIL;

    @Test
    public void addsServiceAccountRolesToExistingPolicy() throws Exception {
        CloudResourceManager resourceManager = mock(CloudResourceManager.class);
        CloudResourceManager.Projects projects = mock(CloudResourceManager.Projects.class);
        CloudResourceManager.Projects.GetIamPolicy getIamPolicy = mock(CloudResourceManager.Projects.GetIamPolicy.class);
        CloudResourceManager.Projects.SetIamPolicy setIamPolicy = mock(CloudResourceManager.Projects.SetIamPolicy.class);
        Policy policy = new Policy().setBindings(new ArrayList<>());
        when(resourceManager.projects()).thenReturn(projects);
        when(projects.getIamPolicy("projects/test", new GetIamPolicyRequest())).thenReturn(getIamPolicy);
        when(getIamPolicy.execute()).thenReturn(policy);

        ArgumentCaptor<String> projectArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SetIamPolicyRequest> policyArgumentCaptor = ArgumentCaptor.forClass(SetIamPolicyRequest.class);
        when(projects.setIamPolicy(projectArgumentCaptor.capture(), policyArgumentCaptor.capture())).thenReturn(setIamPolicy);
        PipelineIamPolicy victim = new PipelineIamPolicy(resourceManager);
        victim.apply(new ServiceAccount().setEmail(EMAIL).setProjectId(PROJECT_ID));
        assertThat(projectArgumentCaptor.getValue()).isEqualTo(PROJECT_ID);
        assertThat(policyArgumentCaptor.getValue().getPolicy().getBindings()).contains(new Binding().setMembers(List.of(IDENTITY))
                        .setRole(PipelineIamPolicy.COMPUTE_ADMIN),
                new Binding().setMembers(List.of(IDENTITY)).setRole(PipelineIamPolicy.STORAGE_ADMIN));
    }

}