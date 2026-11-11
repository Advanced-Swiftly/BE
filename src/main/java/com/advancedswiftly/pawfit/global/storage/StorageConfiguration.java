package com.advancedswiftly.pawfit.global.storage;

import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfiguration {

    @Bean
    StorageClient storageClient(PawfitProperties properties) {
        return new SupabaseStorageClient(properties.getStorage());
    }

    @Bean
    TransactionalStorageService transactionalStorageService(
            StorageClient storageClient, PawfitProperties properties) {
        return new TransactionalStorageService(storageClient, properties.getStorage());
    }
}
