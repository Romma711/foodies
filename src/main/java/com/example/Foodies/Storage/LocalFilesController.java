package com.example.Foodies.Storage;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serves files saved by {@link LocalFileStorage}. With R2 the files are served by Cloudflare instead. */
@RestController
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFilesController {

    private static final String PREFIX = "/api/files/";

    private final LocalFileStorage storage;

    public LocalFilesController(LocalFileStorage storage) {
        this.storage = storage;
    }

    @GetMapping(PREFIX + "**")
    public ResponseEntity<Resource> serve(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith(PREFIX) || path.length() == PREFIX.length()) {
            return ResponseEntity.notFound().build();
        }
        String key = path.substring(PREFIX.length());
        try {
            return storage.find(key)
                    .<ResponseEntity<Resource>>map(file -> ResponseEntity.ok()
                            .contentType(key.endsWith(".pdf") ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM)
                            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().build().toString())
                            .body(new FileSystemResource(file)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
