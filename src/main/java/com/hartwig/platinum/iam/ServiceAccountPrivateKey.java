package com.hartwig.platinum.iam;

import java.io.IOException;

import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.CreateServiceAccountKeyRequest;
import com.google.api.services.iam.v1.model.ServiceAccountKey;

public class ServiceAccountPrivateKey {

    private final Iam iam;

    public ServiceAccountPrivateKey(final Iam iam) {
        this.iam = iam;
    }

    public JsonKey create(final String project, final String email) {
        try {
            ServiceAccountKey key = iam.projects()
                    .serviceAccounts()
                    .keys()
                    .create(ServiceAccountId.from(project, email), new CreateServiceAccountKeyRequest())
                    .execute();
            return JsonKey.of(key.getName(), key.getPrivateKeyData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(final JsonKey key) {
        try {
            iam.projects().serviceAccounts().keys().delete(key.id()).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
