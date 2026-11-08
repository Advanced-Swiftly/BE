package com.advancedswiftly.pawfit.global.storage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StoragePolicyValidatorTest {

    private PawfitProperties.Storage properties;
    private StoragePolicyValidator validator;

    @BeforeEach
    void setUp() {
        properties = new PawfitProperties.Storage();
        properties.setMaxFileSize(4);
        validator = new StoragePolicyValidator(properties);
    }

    @Test
    void acceptsSafeImage() {
        properties.setMaxFileSize(16);
        validator.validate(
                "vaccinations/1/document.png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                "image/png");
    }

    @Test
    void rejectsPathTraversal() {
        assertThatThrownBy(() -> validator.validate("../secret.png", new byte[] {1}, "image/png"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("경로");
    }

    @Test
    void rejectsOversizedFile() {
        assertThatThrownBy(() -> validator.validate("safe.png", new byte[5], "image/png"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("크기");
    }

    @Test
    void rejectsUnsupportedContentType() {
        assertThatThrownBy(() -> validator.validate("safe.exe", new byte[] {1}, "application/octet-stream"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("형식");
    }

    @Test
    void rejectsSpoofedContentType() {
        assertThatThrownBy(() -> validator.validate(
                        "safe.jpg", new byte[] {0x25, 0x50, 0x44, 0x46}, "image/jpeg"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Content-Type");
    }
}
