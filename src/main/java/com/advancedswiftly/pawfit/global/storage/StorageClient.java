package com.advancedswiftly.pawfit.global.storage;

import java.net.URI;
import java.time.Duration;

public interface StorageClient {

    StoredObject upload(String path, byte[] content, String contentType);

    URI createSignedReadUrl(String path, Duration ttl);

    void delete(String path);
}
