package com.ebike.config;

import java.net.URI;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;

@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
public class StorageConfiguration {

    @Bean
    public S3Client s3Client(S3StorageProperties properties) {
        var builder = S3Client.builder()
            .region(Region.of(properties.getRegion()))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isPathStyleAccess())
                .build());

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        if (properties.getAccessKey() != null && !properties.getAccessKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
            ));
        }
        return builder.build();
    }

    @Bean
    public ApplicationRunner initializeStorageBucket(S3Client client, S3StorageProperties properties) {
        return args -> {
            if (!properties.isInitializeBucket()) return;
            try {
                client.headBucket(request -> request.bucket(properties.getBucket()));
            } catch (NoSuchBucketException exception) {
                client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
            }
            String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + properties.getBucket() + "/*\"]}]}";
            client.putBucketPolicy(PutBucketPolicyRequest.builder()
                .bucket(properties.getBucket())
                .policy(policy)
                .build());
        };
    }
}
