package com.example.Foodies.Storage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Cloudflare R2 adapter, through R2's S3-compatible API. */
public class R2FileStorage implements FileStorage {

    private final S3Client s3;
    private final String bucket;
    private final String publicBaseUrl;

    public R2FileStorage(S3Client s3, String bucket, String publicBaseUrl) {
        this.s3 = s3;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void upload(String key, byte[] content, String contentType) {
        s3.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(content));
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public String publicUrl(String key) {
        return publicBaseUrl + "/" + key;
    }
}
