package com.hartwig.platinum.iam;

import java.io.IOException;
import java.util.List;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.Binding;
import com.google.api.services.cloudresourcemanager.model.GetIamPolicyRequest;
import com.google.api.services.cloudresourcemanager.model.Policy;
import com.google.api.services.cloudresourcemanager.model.SetIamPolicyRequest;
import com.google.api.services.iam.v1.model.ServiceAccount;
import com.google.cloud.Identity;

public class PipelineIamPolicy {

    static final String COMPUTE_ADMIN = "roles/compute.admin";
    static final String STORAGE_ADMIN = "roles/storage.admin";
    private final CloudResourceManager cloudResourceManager;

    public PipelineIamPolicy(final CloudResourceManager cloudResourceManager) {
        this.cloudResourceManager = cloudResourceManager;
    }

    void apply(final ServiceAccount serviceAccount) {
        try {
            Policy existingPolicy =
                    cloudResourceManager.projects().getIamPolicy(serviceAccount.getProjectId(), new GetIamPolicyRequest()).execute();
            existingPolicy.getBindings().addAll(List.of(binding(serviceAccount, COMPUTE_ADMIN), binding(serviceAccount, STORAGE_ADMIN)));
            cloudResourceManager.projects()
                    .setIamPolicy(serviceAccount.getProjectId(), new SetIamPolicyRequest().setPolicy(existingPolicy))
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Binding binding(final ServiceAccount serviceAccount, final String role) {
        return new Binding().setRole(role).setMembers(List.of(Identity.serviceAccount(serviceAccount.getEmail()).strValue()));
    }
}
