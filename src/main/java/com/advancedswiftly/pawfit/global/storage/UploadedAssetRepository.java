package com.advancedswiftly.pawfit.global.storage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UploadedAssetRepository extends JpaRepository<UploadedAsset, UUID> {}
