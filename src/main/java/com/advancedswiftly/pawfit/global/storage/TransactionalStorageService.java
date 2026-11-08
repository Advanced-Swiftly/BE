package com.advancedswiftly.pawfit.global.storage;

import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TransactionalStorageService {

    private static final Logger log = LoggerFactory.getLogger(TransactionalStorageService.class);
    private final StorageClient storageClient;
    private final StoragePolicyValidator policyValidator;
    private final PawfitProperties.Storage properties;

    public TransactionalStorageService(StorageClient storageClient, PawfitProperties.Storage properties) {
        this.storageClient = storageClient;
        this.properties = properties;
        this.policyValidator = new StoragePolicyValidator(properties);
    }

    public StoredObject upload(String path, byte[] content, String contentType) {
        policyValidator.validate(path, content, contentType);
        StoredObject storedObject = storageClient.upload(path, content, contentType);
        registerDeleteOnRollback(path);
        return storedObject;
    }

    public URI createSignedReadUrl(String path) {
        policyValidator.validatePath(path);
        return storageClient.createSignedReadUrl(path, properties.getSignedUrlTtl());
    }

    public void deleteAfterCommit(String path) {
        policyValidator.validatePath(path);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            storageClient.delete(path);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storageClient.delete(path);
                } catch (RuntimeException exception) {
                    log.error("Failed to delete storage object after commit. path={}", path, exception);
                }
            }
        });
    }

    private void registerDeleteOnRollback(String path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    storageClient.delete(path);
                } catch (RuntimeException exception) {
                    log.error("Failed to compensate storage upload. path={}", path, exception);
                }
            }
        });
    }
}
