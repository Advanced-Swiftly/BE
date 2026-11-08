package com.advancedswiftly.pawfit.global.storage;

import com.advancedswiftly.pawfit.global.config.PawfitProperties;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.util.regex.Pattern;

public class StoragePolicyValidator {

    private static final Pattern SAFE_PATH = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._/-]*");
    private final PawfitProperties.Storage properties;

    public StoragePolicyValidator(PawfitProperties.Storage properties) {
        this.properties = properties;
    }

    public void validate(String path, byte[] content, String contentType) {
        validatePath(path);
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "빈 파일은 업로드할 수 없습니다.");
        }
        if (content.length > properties.getMaxFileSize()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        if (contentType == null || !properties.getAllowedContentTypes().contains(contentType)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        if (!matchesFileSignature(content, contentType)) {
            throw new BusinessException(
                    ErrorCode.UNSUPPORTED_FILE_TYPE,
                    "파일 내용과 Content-Type이 일치하지 않습니다.");
        }
    }

    public void validatePath(String path) {
        if (path == null
                || path.isBlank()
                || path.startsWith("/")
                || path.contains("..")
                || !SAFE_PATH.matcher(path).matches()) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "안전하지 않은 파일 경로입니다.");
        }
    }

    private boolean matchesFileSignature(byte[] content, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> startsWith(content, new int[] {0xFF, 0xD8, 0xFF});
            case "image/png" -> startsWith(content, new int[] {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "image/webp" -> startsWith(content, new int[] {0x52, 0x49, 0x46, 0x46})
                    && content.length >= 12
                    && content[8] == 0x57
                    && content[9] == 0x45
                    && content[10] == 0x42
                    && content[11] == 0x50;
            case "application/pdf" -> startsWith(content, new int[] {0x25, 0x50, 0x44, 0x46, 0x2D});
            default -> false;
        };
    }

    private boolean startsWith(byte[] content, int[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((content[index] & 0xFF) != signature[index]) {
                return false;
            }
        }
        return true;
    }
}
