package com.advancedswiftly.pawfit.global.storage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.advancedswiftly.pawfit.business.application.ManagedStoreChecker;
import com.advancedswiftly.pawfit.global.error.BusinessException;

import org.junit.jupiter.api.Test;

import java.util.*;

class FileServiceTest {
    @Test
    void anotherUsersUploadCannotBeLinkedOrRead() {
        var storage = mock(TransactionalStorageService.class);
        var assets = mock(UploadedAssetRepository.class);
        var id = UUID.randomUUID();
        when(assets.findById(id))
                .thenReturn(
                        Optional.of(
                                new UploadedAsset(id, 2L, "uploads/2/a", "image/png", "a.png")));
        var service =
                new FileService(
                        storage,
                        assets,
                        mock(StoreImageRepository.class),
                        mock(ManagedStoreChecker.class));
        assertThatThrownBy(() -> service.readUrl(1L, id, false))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.validateReferences(1L, List.of("/api/v1/files/" + id)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(storage);
    }
}
