package net.hyf.image_upload_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class ObjectStorageService {

    private final S3Client s3Client;

    @Value("${storage.bucket-name}")
    private String bucketName;

    public void upload(
            String storageKey,
            byte[] fileBytes,
            String contentType
    ) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .contentType(contentType)
                .contentLength((long) fileBytes.length)
                .build();

        s3Client.putObject(
                request,
                RequestBody.fromBytes(fileBytes)
        );
    }

    public byte[] download(String storageKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .build();

        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    public void delete(String storageKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .build();

        s3Client.deleteObject(request);
    }
}