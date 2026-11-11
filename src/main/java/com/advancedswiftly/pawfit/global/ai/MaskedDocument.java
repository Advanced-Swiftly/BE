package com.advancedswiftly.pawfit.global.ai;

import java.util.Arrays;

public record MaskedDocument(byte[] content, String contentType) {

    public MaskedDocument {
        content = content == null ? new byte[0] : Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
