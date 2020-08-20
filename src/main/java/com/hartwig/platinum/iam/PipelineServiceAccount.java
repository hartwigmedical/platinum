package com.hartwig.platinum.iam;

import java.io.IOException;
import java.util.List;

import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.Binding;
import com.google.api.services.iam.v1.model.CreateServiceAccountRequest;
import com.google.api.services.iam.v1.model.ListServiceAccountsResponse;
import com.google.api.services.iam.v1.model.Policy;
import com.google.api.services.iam.v1.model.ServiceAccount;
import com.google.api.services.iam.v1.model.SetIamPolicyRequest;
import com.google.cloud.Identity;
import com.google.common.collect.ImmutableList;

public class PipelineServiceAccount {

    private final Iam iam;
    private final PipelineIamPolicy policy;

    public PipelineServiceAccount(final Iam iam, final PipelineIamPolicy policy) {
        this.iam = iam;
        this.policy = policy;
    }

    public String findOrCreate(final String project, final String runName) {
        try {
            String serviceAccountName = ServiceAccountName.from(runName);
            String projectResourceName = "projects/" + project;
            List<ServiceAccount> accounts = projectServiceAccounts(iam, projectResourceName);
            ServiceAccount serviceAccount = accounts != null ? accounts.stream()
                    .filter(s -> serviceAccountName(s).equals(serviceAccountName))
                    .findFirst()
                    .orElse(null) : null;
            if (serviceAccount == null) {
                serviceAccount = iam.projects()
                        .serviceAccounts()
                        .create(projectResourceName, new CreateServiceAccountRequest().setAccountId(serviceAccountName))
                        .execute();
            }
            policy.apply(serviceAccount);
            return serviceAccount.getEmail();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(final String project, final String email) {
        try {
            iam.projects().serviceAccounts().delete(String.format("projects/%s/serviceAccounts/%s", project, email)).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String serviceAccountName(final ServiceAccount s) {
        return s.getEmail().split("@")[0];
    }

    private static List<ServiceAccount> projectServiceAccounts(final Iam iam, final String projectResourceName) throws IOException {
        ListServiceAccountsResponse response = iam.projects().serviceAccounts().list(projectResourceName).execute();
        List<ServiceAccount> accounts = response.getAccounts();
        while (response.getNextPageToken() != null) {
            response = iam.projects()
                    .serviceAccounts()
                    .list(projectResourceName)
                    .setPageSize(100)
                    .setPageToken(response.getNextPageToken())
                    .execute();
            accounts.addAll(response.getAccounts());
        }
        return accounts;
    }
}
