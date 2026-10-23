package com.example.Foodies.Storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
    public LocalFileStorage localFileStorage(StorageProperties properties) {
        StorageProperties.Local local = properties.local();
        String dir = local != null && hasText(local.dir()) ? local.dir() : "uploads";
        String publicBaseUrl = local != null && hasText(local.publicBaseUrl()) ? local.publicBaseUrl() : "/api/files";
        return new LocalFileStorage(Path.of(dir), publicBaseUrl);
    }

    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "r2")
    public FileStorage r2FileStorage(StorageProperties properties) {
        StorageProperties.R2 r2 = properties.r2();
        requireConfigured(r2);
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create("https://" + r2.accountId() + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.accessKeyId(), r2.secretAccessKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                // R2 does not support every checksum the SDK sends by default; only send them when required.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
        return new R2FileStorage(client, r2.bucket(), r2.publicBaseUrl());
    }

    /** Fails at startup, naming the missing variables, instead of failing on the first upload. */
    private static void requireConfigured(StorageProperties.R2 r2) {
        Map<String, String> required = new LinkedHashMap<>();
        required.put("R2_ACCOUNT_ID", r2 == null ? null : r2.accountId());
        required.put("R2_ACCESS_KEY_ID", r2 == null ? null : r2.accessKeyId());
        required.put("R2_SECRET_ACCESS_KEY", r2 == null ? null : r2.secretAccessKey());
        required.put("R2_BUCKET", r2 == null ? null : r2.bucket());
        required.put("R2_PUBLIC_BASE_URL", r2 == null ? null : r2.publicBaseUrl());
        var missing = required.entrySet().stream().filter(e -> !hasText(e.getValue())).map(Map.Entry::getKey).toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("storage.provider=r2 requiere las variables: " + String.join(", ", missing));
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
