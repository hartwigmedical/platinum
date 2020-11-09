package com.hartwig.platinum.iam;

import java.io.IOException;

import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.model.CreateServiceAccountKeyRequest;
import com.google.api.services.iam.v1.model.ServiceAccountKey;

public class EphemeralServiceAccountPrivateKey implements ServiceAccountPrivateKey {

    private final Iam iam;

    public EphemeralServiceAccountPrivateKey(final Iam iam) {
        this.iam = iam;
    }

    public JsonKey create(final String project, final String email) {
        try {
            ServiceAccountKey key = iam.projects()
                    .serviceAccounts()
                    .keys()
                    .create(ServiceAccountId.from(project, email), new CreateServiceAccountKeyRequest())
                    .execute();
            return JsonKey.of(key.getPrivateKeyData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
