package com.hartwig.platinum.iam;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.iam.v1.Iam;
import com.hartwig.platinum.config.PlatinumConfiguration;

public interface PipelineServiceAccount {

    String findOrCreate();

    void delete(final String email);

    static PipelineServiceAccount from(final Iam iam, final CloudResourceManager cloudResourceManager, final String runName,
            final String project, final PlatinumConfiguration configuration) {
        return configuration.serviceAccount().<PipelineServiceAccount>map(s -> new PipelineServiceAccount() {
            @Override
            public String findOrCreate() {
                return s;
            }

            @Override
            public void delete(final String email) {
                // do nothing
            }
        }).orElse(new TransientPipelineServiceAccount(iam, new PipelineIamPolicy(cloudResourceManager), runName, project));
    }
}
