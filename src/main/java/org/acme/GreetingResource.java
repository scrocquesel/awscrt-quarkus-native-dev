package org.acme;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Path("/hello")
public class GreetingResource {

    @Inject
    @ConfigProperty(name = "quarkus.dynamodb.endpoint-override")
    String endpoint;

    @Inject
    @ConfigProperty(name = "quarkus.dynamodb.aws.region")
    String region;

    @Inject
    @ConfigProperty(name = "quarkus.dynamodb.aws.access-key-id")
    String accessKey;

    @Inject
    @ConfigProperty(name = "quarkus.dynamodb.aws.secret-access-key")
    String secretKey;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {

        DynamoDbAsyncClient dynamoDbClient = DynamoDbAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .httpClientBuilder(AwsCrtAsyncHttpClient.builder())
                .build();

        List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        attributeDefinitions
                .add(AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build());
        attributeDefinitions.add(
                AttributeDefinition.builder().attributeName("value").attributeType(ScalarAttributeType.S).build());

        List<KeySchemaElement> ks = new ArrayList<>();
        ks.add(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build());
        ks.add(KeySchemaElement.builder().attributeName("value").keyType(KeyType.RANGE).build());

        ProvisionedThroughput provisionedthroughput = ProvisionedThroughput.builder().readCapacityUnits(1000L)
                .writeCapacityUnits(1000L).build();

        dynamoDbClient.createTable(b -> b.tableName("test").attributeDefinitions(attributeDefinitions).keySchema(ks).provisionedThroughput(provisionedthroughput))
                .join();

        try {
            Map<String, AttributeValue> itemAttributes = new HashMap<>();
            itemAttributes.put("id", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
            itemAttributes.put("value", AttributeValue.builder().s("test").build());

            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName("test")
                    .item(itemAttributes)
                    .build()).get();
            return "Hello RESTEasy";
        } catch (DynamoDbException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}