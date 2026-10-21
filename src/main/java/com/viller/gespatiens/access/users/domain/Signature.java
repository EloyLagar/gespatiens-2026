package com.viller.gespatiens.access.users.domain;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

// La firma escaneada de un empleado, en PNG. La memoria final: "el empleado por cuenta propia suba una
// imagen de su firma la cual se empleará para los informes"
public class Signature {

    // Una firma escaneada ocupa unas decenas de KB: 1 MB deja margen sin llenar la base de datos
    public static final int MAX_BYTES = 1024 * 1024;
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};

    private final UUID userId;
    private final byte[] png;
    private final Instant uploadedAt;

    private Signature(UUID userId, byte[] png, Instant uploadedAt) {
        if (userId == null || uploadedAt == null) {
            throw new IllegalArgumentException("La firma necesita empleado y fecha");
        }
        checkPng(png);
        this.userId = userId;
        this.png = png.clone();
        this.uploadedAt = uploadedAt;
    }

    public static Signature upload(UUID userId, byte[] png, Instant now) {
        return new Signature(userId, png, now);
    }

    public static Signature reconstitute(UUID userId, byte[] png, Instant uploadedAt) {
        return new Signature(userId, png, uploadedAt);
    }

    // Un PNG de verdad que se pueda dibujar en el informe, no solo un fichero con la extensión cambiada
    private static void checkPng(byte[] png) {
        if (png == null || png.length == 0) {
            throw new IllegalArgumentException("Sube la imagen de tu firma");
        }
        if (png.length > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen de la firma no puede pasar de 1 MB");
        }
        if (png.length < PNG_MAGIC.length || !Arrays.equals(Arrays.copyOf(png, PNG_MAGIC.length), PNG_MAGIC)) {
            throw new IllegalArgumentException("La firma tiene que ser una imagen PNG");
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                throw new IllegalArgumentException("La imagen de la firma está dañada");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("La imagen de la firma está dañada", e);
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public byte[] getPng() {
        return png.clone();
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
