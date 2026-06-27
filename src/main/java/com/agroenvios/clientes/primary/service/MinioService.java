package com.agroenvios.clientes.primary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Service
@Slf4j
public class MinioService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final String publicEndpoint;

    public MinioService(
            @Value("${aws.endpoint}") String endpoint,
            @Value("${aws.public-endpoint}") String publicEndpoint,
            @Value("${aws.accessKeyId}") String accessKey,
            @Value("${aws.secretKey}") String secretKey,
            @Value("${aws.region}") String region,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.disable-ssl-verification:false}") boolean disableSslVerification
    ) {
        this.publicEndpoint = publicEndpoint;
        this.bucket = bucket;

        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));

        S3Configuration s3Config = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        SdkHttpClient httpClient;
        if (disableSslVerification) {
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }};
            httpClient = UrlConnectionHttpClient.builder()
                    .tlsTrustManagersProvider(() -> trustAll)
                    .build();
            log.warn("MinioService: verificación SSL deshabilitada");
        } else {
            httpClient = UrlConnectionHttpClient.create();
        }

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Config)
                .httpClient(httpClient)
                .build();

        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Config)
                .build();

        log.info("MinioService inicializado con endpoint: {} bucket: {}", endpoint, bucket);
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

    public String generatePresignedUrl(String objectKey, String targetBucket) {
        if (objectKey == null || objectKey.isBlank()) return null;
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(targetBucket)
                            .key(objectKey)
                            .build())
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Error generando URL prefirmada para key: {}, bucket: {}: {}", objectKey, targetBucket, e.getMessage());
            return null;
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
