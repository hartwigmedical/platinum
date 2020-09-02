package com.hartwig.platinum.kubernetes;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.container.v1beta1.Container;
import com.google.api.services.container.v1beta1.ContainerScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

public class ContainerProvider {

    public static Container get() {
        try {
            return new Container.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault()
                            .createScoped(Set.of(ContainerScopes.CLOUD_PLATFORM)))).setApplicationName("platinum").build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
