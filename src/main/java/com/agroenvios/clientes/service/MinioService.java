package com.agroenvios.clientes.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Service
@Slf4j
public class MinioService {

    private final S3Client s3Client;
    private final String bucket;
    private final String endpoint;
    private final String publicEndpoint;

    public MinioService(
            @Value("${aws.endpoint}") String endpoint,
            @Value("${aws.public-endpoint}") String publicEndpoint,
            @Value("${aws.accessKeyId}") String accessKey,
            @Value("${aws.secretKey}") String secretKey,
            @Value("${aws.region}") String region,
            @Value("${aws.s3.bucket}") String bucket
    ) {
        this.endpoint = endpoint;
        this.publicEndpoint = publicEndpoint;
        this.bucket = bucket;
        log.info("MinioService inicializado con endpoint: {} publicEndpoint: {} bucket: {}", endpoint, publicEndpoint, bucket);
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .forcePathStyle(true)
                .build();
    }

    public String upload(MultipartFile file, String folder, String name) {
        String extension = getExtension(file.getOriginalFilename());
        String key = folder + "/" + name + "." + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (Exception e) {
            log.error("Error al subir archivo a MinIO: {}", e.getMessage());
            throw new RuntimeException("Error al subir archivo");
        }

        return publicEndpoint + "/" + bucket + "/" + key;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
