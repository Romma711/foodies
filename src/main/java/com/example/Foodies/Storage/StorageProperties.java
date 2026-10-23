package com.example.Foodies.Storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(String provider, Local local, R2 r2) {

    public record Local(String dir, String publicBaseUrl) {
    }

    public record R2(String accountId, String accessKeyId, String secretAccessKey, String bucket, String publicBaseUrl) {
    }
}
