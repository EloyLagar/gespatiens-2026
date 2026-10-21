package com.viller.gespatiens.access.users.domain;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignatureTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");

    static byte[] png() throws IOException {
        BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_ARGB);
        image.getGraphics().drawLine(5, 30, 110, 10);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void aScannedPngIsAccepted() throws IOException {
        byte[] png = png();

        Signature signature = Signature.upload(UUID.randomUUID(), png, NOW);

        assertArrayEquals(png, signature.getPng());
    }

    // Solo PNG de verdad: ni otro formato con la extensión cambiada ni un fichero dañado
    @Test
    void anythingButAValidPngIsRejected() throws IOException {
        UUID id = UUID.randomUUID();
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0, 0, 0, 0, 0};
        byte[] truncated = Arrays.copyOf(png(), 20);

        assertThrows(IllegalArgumentException.class, () -> Signature.upload(id, new byte[0], NOW));
        assertThrows(IllegalArgumentException.class, () -> Signature.upload(id, jpeg, NOW));
        assertThrows(IllegalArgumentException.class, () -> Signature.upload(id, truncated, NOW));
    }

    @Test
    void itCannotBeBiggerThanOneMegabyte() {
        byte[] huge = new byte[Signature.MAX_BYTES + 1];

        assertThrows(IllegalArgumentException.class, () -> Signature.upload(UUID.randomUUID(), huge, NOW));
    }
}
