package com.advancedswiftly.pawfit.global.storage;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "uploaded_asset")
public class UploadedAsset {
    @Id
    @Column(name = "asset_id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "storage_path", nullable = false, length = 500, unique = true)
    private String path;

    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    protected UploadedAsset() {}

    public UploadedAsset(
            UUID id, Long userId, String path, String contentType, String originalName) {
        this.id = id;
        this.userId = userId;
        this.path = path;
        this.contentType = contentType;
        this.originalName = originalName;
    }

    public UUID getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPath() {
        return path;
    }

    public String getContentType() {
        return contentType;
    }
}
