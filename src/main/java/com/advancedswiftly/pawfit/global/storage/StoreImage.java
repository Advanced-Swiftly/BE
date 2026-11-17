package com.advancedswiftly.pawfit.global.storage;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "store_image")
public class StoreImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected StoreImage() {}

    public StoreImage(Long storeId, UUID assetId) {
        this.storeId = storeId;
        this.assetId = assetId;
    }

    public UUID getAssetId() {
        return assetId;
    }
}
