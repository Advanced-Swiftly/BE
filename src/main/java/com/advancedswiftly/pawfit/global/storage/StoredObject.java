package com.advancedswiftly.pawfit.global.storage;

public record StoredObject(String bucket, String path, String contentType, long size) {
}
