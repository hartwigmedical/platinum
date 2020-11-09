package com.hartwig.platinum.iam;

import java.io.IOException;
import java.util.List;

import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.CreateServiceAccountRequest;
import com.google.api.services.iam.v1.model.ListServiceAccountsResponse;
import com.google.api.services.iam.v1.model.ServiceAccount;

public class EphemeralPipelineServiceAccount implements PipelineServiceAccount {

    private final Iam iam;
    private final PipelineIamPolicy policy;
    private final String runName;
    private final String project;

    public EphemeralPipelineServiceAccount(final Iam iam, final PipelineIamPolicy policy, final String runName, final String project) {
        this.iam = iam;
        this.policy = policy;
        this.runName = runName;
        this.project = project;
    }

    public String findOrCreate() {
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

    public void delete(final String email) {
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
            if (response.getAccounts() != null) {
                accounts.addAll(response.getAccounts());
            }
        }
        return accounts;
    }
}
