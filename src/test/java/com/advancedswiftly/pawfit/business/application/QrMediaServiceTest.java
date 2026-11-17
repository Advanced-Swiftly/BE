package com.advancedswiftly.pawfit.business.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.advancedswiftly.pawfit.global.storage.TransactionalStorageService;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

class QrMediaServiceTest {
    @Test
    void uploadsAnImageWhoseDecodedPayloadIsTheVisitToken() throws Exception {
        var storage = mock(TransactionalStorageService.class);
        var service = new QrMediaService(storage);
        String reference = service.create("unguessable-demo-qr-token");
        var bytes = ArgumentCaptor.forClass(byte[].class);
        verify(storage).upload(eq(reference.substring(10)), bytes.capture(), eq("image/png"));
        var image = ImageIO.read(new ByteArrayInputStream(bytes.getValue()));
        String decoded =
                new MultiFormatReader()
                        .decode(
                                new BinaryBitmap(
                                        new HybridBinarizer(
                                                new BufferedImageLuminanceSource(image))))
                        .getText();
        assertThat(decoded).isEqualTo("unguessable-demo-qr-token");
    }
}
