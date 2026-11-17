package com.advancedswiftly.pawfit.global.storage;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FileController {
    private final FileService files;

    public FileController(FileService files) {
        this.files = files;
    }

    @PostMapping(value = "/files", consumes = "multipart/form-data")
    public FileService.Uploaded upload(
            Authentication auth, @RequestPart("file") MultipartFile file) {
        return files.upload(Long.valueOf(auth.getName()), file, null);
    }

    @PreAuthorize("hasRole('BUSINESS')")
    @PostMapping(value = "/business/stores/{storeId}/images", consumes = "multipart/form-data")
    public FileService.Uploaded storeImage(
            Authentication auth,
            @PathVariable Long storeId,
            @RequestPart("file") MultipartFile file) {
        return files.upload(Long.valueOf(auth.getName()), file, storeId);
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<Void> read(Authentication auth, @PathVariable UUID id) {
        return ResponseEntity.status(302).location(URI.create(url(auth, id))).build();
    }

    @GetMapping("/files/{id}/url")
    public Map<String, String> readUrl(Authentication auth, @PathVariable UUID id) {
        return Map.of("url", url(auth, id));
    }

    private String url(Authentication auth, UUID id) {
        return files.readUrl(
                Long.valueOf(auth.getName()),
                id,
                auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR")));
    }
}
