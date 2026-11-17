package com.advancedswiftly.pawfit.business.application;

import com.advancedswiftly.pawfit.global.storage.TransactionalStorageService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
public class QrMediaService {
    private final TransactionalStorageService storage;

    public QrMediaService(TransactionalStorageService storage) {
        this.storage = storage;
    }

    public String create(String token) {
        try {
            var matrix = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 480, 480);
            var output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            String path = "qr/" + UUID.randomUUID() + ".png";
            storage.upload(path, output.toByteArray(), "image/png");
            return "storage://" + path;
        } catch (com.google.zxing.WriterException | java.io.IOException e) {
            throw new IllegalStateException("QR 이미지 생성 실패", e);
        }
    }

    public String url(String reference) {
        return reference.startsWith("storage://")
                ? storage.createSignedReadUrl(reference.substring(10)).toString()
                : reference;
    }
}
