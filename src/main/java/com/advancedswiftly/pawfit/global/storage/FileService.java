package com.advancedswiftly.pawfit.global.storage;

import com.advancedswiftly.pawfit.business.application.ManagedStoreChecker;
import com.advancedswiftly.pawfit.global.error.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {
    private final TransactionalStorageService storage;
    private final UploadedAssetRepository assets;
    private final StoreImageRepository images;
    private final ManagedStoreChecker managedStores;

    public FileService(
            TransactionalStorageService storage,
            UploadedAssetRepository assets,
            StoreImageRepository images,
            ManagedStoreChecker managedStores) {
        this.storage = storage;
        this.assets = assets;
        this.images = images;
        this.managedStores = managedStores;
    }

    @Transactional
    public Uploaded upload(Long userId, MultipartFile file, Long storeId) {
        if (storeId != null) managedStores.requireManagedStore(userId, storeId);
        String type = file.getContentType();
        if (storeId != null && (type == null || !type.startsWith("image/")))
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        UUID id = UUID.randomUUID();
        String path = "uploads/" + userId + "/" + id;
        try {
            storage.upload(path, file.getBytes(), type);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        String name = file.getOriginalFilename();
        if (name == null) name = "attachment";
        if (name.length() > 255) name = name.substring(name.length() - 255);
        assets.save(new UploadedAsset(id, userId, path, type, name));
        if (storeId != null) images.save(new StoreImage(storeId, id));
        return new Uploaded(
                id, "/api/v1/files/" + id, storage.createSignedReadUrl(path).toString());
    }

    @Transactional(readOnly = true)
    public String readUrl(Long userId, UUID id, boolean operator) {
        UploadedAsset asset =
                assets.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!operator && !asset.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        return storage.createSignedReadUrl(asset.getPath()).toString();
    }

    @Transactional(readOnly = true)
    public List<String> storeImages(Long storeId) {
        return images.findByStoreIdOrderByDisplayOrderAscIdAsc(storeId).stream()
                .map(i -> assets.findById(i.getAssetId()).orElseThrow())
                .map(a -> storage.createSignedReadUrl(a.getPath()).toString())
                .toList();
    }

    private UUID referenceId(String reference) {
        var match =
                java.util.regex.Pattern.compile(
                                "^(?:https?://[^/]+)?/api/v1/files/([0-9a-fA-F-]{36})$")
                        .matcher(reference);
        if (!match.matches()) return null;
        try {
            return UUID.fromString(match.group(1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public void validateReferences(Long userId, List<String> references) {
        if (references == null) return;
        for (String reference : references) {
            UUID id = referenceId(reference);
            if (id == null) continue;
            var asset =
                    assets.findById(id)
                            .filter(a -> a.getUserId().equals(userId))
                            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        }
    }

    @Transactional(readOnly = true)
    public List<String> resolveReferences(List<String> references) {
        return references.stream()
                .map(
                        reference -> {
                            UUID id = referenceId(reference);
                            if (id == null) return reference;
                            return assets.findById(id)
                                    .map(a -> storage.createSignedReadUrl(a.getPath()).toString())
                                    .orElse(reference);
                        })
                .toList();
    }

    public record Uploaded(UUID assetId, String url, String signedUrl) {}
}
