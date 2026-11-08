package com.advancedswiftly.pawfit.global.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TransactionalStorageServiceTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void removesUploadedObjectWhenDatabaseTransactionRollsBack() {
        FakeStorageClient client = new FakeStorageClient();
        TransactionalStorageService service = new TransactionalStorageService(
                client, new PawfitProperties.Storage());
        TransactionSynchronizationManager.initSynchronization();

        service.upload(
                "vaccinations/1/file.png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                "image/png");
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(client.deletedPaths).containsExactly("vaccinations/1/file.png");
    }

    @Test
    void deletesExistingObjectOnlyAfterDatabaseCommit() {
        FakeStorageClient client = new FakeStorageClient();
        TransactionalStorageService service = new TransactionalStorageService(
                client, new PawfitProperties.Storage());
        TransactionSynchronizationManager.initSynchronization();

        service.deleteAfterCommit("vaccinations/1/old.png");
        assertThat(client.deletedPaths).isEmpty();

        TransactionSynchronizationManager.getSynchronizations().forEach(
                TransactionSynchronization::afterCommit);
        assertThat(client.deletedPaths).containsExactly("vaccinations/1/old.png");
    }

    private static final class FakeStorageClient implements StorageClient {

        private final List<String> deletedPaths = new ArrayList<>();

        @Override
        public StoredObject upload(String path, byte[] content, String contentType) {
            return new StoredObject("pawfit", path, contentType, content.length);
        }

        @Override
        public URI createSignedReadUrl(String path, Duration ttl) {
            return URI.create("https://example.com/signed");
        }

        @Override
        public void delete(String path) {
            deletedPaths.add(path);
        }
    }
}
