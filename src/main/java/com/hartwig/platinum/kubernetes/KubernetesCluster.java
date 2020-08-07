package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.hartwig.platinum.config.PipelineConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.generic.GenericKubernetesApi;

public class KubernetesCluster {
    private final String runName;
    private final String name;
    private final String namespace;
    private String outputBucket;
    private final CoreV1Api api;

    private KubernetesCluster(final String runName, final String outputBucket, final CoreV1Api api) {
        this.runName = runName;
        this.name = format("platinum-%s-cluster", runName);
        this.namespace = "platinum-" + runName;
        this.outputBucket = outputBucket;
        this.api = api;
    }

    public void submit(final PlatinumConfiguration configuration) {
        //api.createNamespace()
        List<PipelineConfiguration> pipelines = new ArrayList<>();
        for (String sample: configuration.samples().keySet()) {
            pipelines.add(PipelineConfiguration.builder()
                    .sampleName(sample)
                    .putAllArguments(configuration.pipelineArguments())
                    .putArguments("-set_id", sample)
                    .putArguments("-sample_json", format("/secrets/samples/%s.json", sample))
                    .putArguments("-patient_report_bucket", outputBucket)
                    .putArguments("-private_key_path", "")
                    .build());
        }
        for (PipelineConfiguration pipeline: pipelines) {
//            V1Pod pod = new V1Pod();
//            pod.
//            api.createNamespacedPod(namespace, pod, );
            List<String> arguments = new ArrayList<>();
            arguments.add("/pipeline5.sh");
            for (Map.Entry<String, String> argumentPair: pipeline.arguments().entrySet()) {
                arguments.add(argumentPair.getKey());
                arguments.add(argumentPair.getValue());
            }
            String[] args = arguments.toArray(new String[]{});
            String podName = format("%s-%s", namespace, pipeline.sampleName());

            // Messing around

            V1Pod pod =
                    new V1Pod()
                            .metadata(new V1ObjectMeta().name(podName).namespace(namespace))
                            .spec(
                                    new V1PodSpec()
                                            .containers(Arrays.asList(new V1Container().name("c").image("hartwigmedicalfoundation/pipeline5:5.13.1672"))));

            GenericKubernetesApi<V1Pod, V1PodList> podClient =
                    new GenericKubernetesApi<>(V1Pod.class, V1PodList.class, "", "v1", "pods", api.getApiClient());

            V1Pod latestPod = podClient.create(pod).getObject();
//                            .onFailure(
//                                    errorStatus -> {
//                                        System.out.println("Not Created!");
//                                        throw new RuntimeException(errorStatus.toString());
//                                    })
//                            .getObject();

            System.out.println(latestPod.getStatus());
            System.out.println("Created!");



            // Client errors, doesn't want to leave localhost
//            Exec exec = new Exec();
//
//            try {
//                exec.exec(namespace, podName, args, true, false);
//
//            } catch (Exception e) {
//                throw new RuntimeException("Failed to execute pod", e);
//            }
        }
    }

    public static KubernetesCluster findOrCreate(final String runName, final String outputBucket) {
        try {
//            ApiClient apiClient = Config.defaultClient();
            ApiClient apiClient = ClientBuilder.standard().build();

//            apiClient.buildUrl("https://35.204.252.11", null, null);
            return new KubernetesCluster(runName, outputBucket, new CoreV1Api(apiClient));
        } catch (Exception e) {
            throw new RuntimeException("Unable to create cluster", e);
        }
    }
}
