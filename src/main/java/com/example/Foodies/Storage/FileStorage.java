package com.example.Foodies.Storage;

/**
 * Port for storing files outside the database. Only the object key is persisted;
 * the public URL is derived from it so the provider or domain can change without migrating rows.
 */
public interface FileStorage {

    void upload(String key, byte[] content, String contentType);

    void delete(String key);

    String publicUrl(String key);
}
