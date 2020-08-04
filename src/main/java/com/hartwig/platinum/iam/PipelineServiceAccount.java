package com.hartwig.platinum.iam;

import java.io.IOException;
import java.util.List;

import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.CreateServiceAccountRequest;
import com.google.api.services.iam.v1.model.ListServiceAccountsResponse;
import com.google.api.services.iam.v1.model.ServiceAccount;

public class PipelineServiceAccount {

    private final Iam iam;

    public PipelineServiceAccount(final Iam iam) {
        this.iam = iam;
    }

    public String findOrCreate(final String project, final String runName) {
        try {
            String serviceAccountId = ServiceAccountId.from(project, runName);
            String projectResourceName = "projects/" + project;
            List<ServiceAccount> accounts = projectServiceAccounts(iam, projectResourceName);
            ServiceAccount serviceAccount =
                    accounts.stream().filter(s -> s.getUniqueId().equals(serviceAccountId)).findFirst().orElse(null);
            if (serviceAccount == null) {
                serviceAccount = iam.projects()
                        .serviceAccounts()
                        .create(projectResourceName, new CreateServiceAccountRequest().setAccountId(serviceAccountId))
                        .execute();
            }
            return serviceAccount.getEmail();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public void delete() {

    }
}
