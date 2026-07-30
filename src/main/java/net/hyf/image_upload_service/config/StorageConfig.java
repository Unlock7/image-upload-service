package net.hyf.image_upload_service.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class StorageConfig {

    @Bean
    public S3Client s3Client(
            @Value("${storage.endpoint}") String endpoint,
            @Value("${storage.region}") String region,
            @Value("${storage.key-id}") String keyId,
            @Value("${storage.application-key}") String applicationKey
    ) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        keyId,
                                        applicationKey
                                )
                        )
                )
                .build();
    }
}