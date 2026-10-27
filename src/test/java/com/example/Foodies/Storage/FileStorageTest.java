package com.example.Foodies.Storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FileStorageTest {

    @Test
    void r2UploadsToTheConfiguredBucketWithTheContentType() throws Exception {
        S3Client s3 = mock(S3Client.class);
        R2FileStorage storage = new R2FileStorage(s3, "foodies-cartas", "https://cdn.example.com/");

        storage.upload("cartas/1/a.pdf", new byte[]{1, 2, 3}, "application/pdf");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3).putObject(request.capture(), body.capture());
        assertThat(request.getValue().bucket()).isEqualTo("foodies-cartas");
        assertThat(request.getValue().key()).isEqualTo("cartas/1/a.pdf");
        assertThat(request.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(body.getValue().contentStreamProvider().newStream().readAllBytes()).containsExactly(1, 2, 3);
    }

    @Test
    void r2DeletesByKeyAndBuildsPublicUrlsFromTheBaseUrl() {
        S3Client s3 = mock(S3Client.class);
        R2FileStorage storage = new R2FileStorage(s3, "foodies-cartas", "https://cdn.example.com/");

        storage.delete("cartas/1/a.pdf");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("foodies-cartas");
        assertThat(request.getValue().key()).isEqualTo("cartas/1/a.pdf");
        assertThat(storage.publicUrl("cartas/1/a.pdf")).isEqualTo("https://cdn.example.com/cartas/1/a.pdf");
    }

    @Test
    void r2ProviderFailsFastNamingTheMissingVariables() {
        StorageProperties properties = new StorageProperties("r2", null,
                new StorageProperties.R2("account", "", "secret", "bucket", " "));

        assertThatThrownBy(() -> new StorageConfig().r2FileStorage(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("R2_ACCESS_KEY_ID")
                .hasMessageContaining("R2_PUBLIC_BASE_URL")
                .hasMessageNotContaining("R2_BUCKET");
    }

    @Test
    void localStorageRejectsKeysOutsideItsRoot(@TempDir Path root) {
        LocalFileStorage storage = new LocalFileStorage(root, "/api/files");

        assertThatThrownBy(() -> storage.upload("../escape.pdf", new byte[]{1}, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(root.getParent().resolve("escape.pdf")).doesNotExist();
    }

    @Test
    void localStorageWritesFindsAndDeletesFiles(@TempDir Path root) {
        LocalFileStorage storage = new LocalFileStorage(root, "/api/files/");

        storage.upload("cartas/7/menu.pdf", new byte[]{9}, "application/pdf");

        assertThat(storage.find("cartas/7/menu.pdf")).isPresent();
        assertThat(storage.publicUrl("cartas/7/menu.pdf")).isEqualTo("/api/files/cartas/7/menu.pdf");
        storage.delete("cartas/7/menu.pdf");
        assertThat(storage.find("cartas/7/menu.pdf")).isEmpty();
    }
}
