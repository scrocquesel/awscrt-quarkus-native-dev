package org.acme;

import java.util.Map;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class LocalStackResource implements QuarkusTestResourceLifecycleManager {

    private LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:1.0.3"))
    .withServices(LocalStackContainer.Service.DYNAMODB);

    @Override
    public Map<String, String> start() {
        localstack.start();

        String endpoint = localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString();
        String region = localstack.getRegion();
        String accessKey = localstack.getAccessKey();
        String secretKey = localstack.getSecretKey();

        Map<String, String> properties = Map.of(
                "quarkus.dynamodb.endpoint-override", endpoint,
                "quarkus.dynamodb.aws.region", region,
                "quarkus.dynamodb.aws.access-key-id", accessKey,
                "quarkus.dynamodb.aws.secret-access-key", secretKey);

        return properties;
    }

    @Override
    public void stop() {
        localstack.stop();
    }
    
}
