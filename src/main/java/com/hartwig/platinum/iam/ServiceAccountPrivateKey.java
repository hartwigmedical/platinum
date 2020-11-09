package com.hartwig.platinum.iam;

import com.google.api.services.iam.v1.Iam;
import com.hartwig.platinum.Console;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.config.ServiceAccountConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ServiceAccountPrivateKey {

    Logger LOGGER = LoggerFactory.getLogger(ServiceAccountPrivateKey.class);

    JsonKey create(final String project, final String email);

    static ServiceAccountPrivateKey from(final PlatinumConfiguration configuration, final Iam iam) {
        return configuration.serviceAccount()
                .flatMap(ServiceAccountConfiguration::existingSecret).<ServiceAccountPrivateKey>map(e -> (project, email) -> {
                    LOGGER.info("Using existing private key for service account {}", Console.bold(email));
                    return JsonKey.existing(e);
                }).orElse(new EphemeralServiceAccountPrivateKey(iam));
    }
}
