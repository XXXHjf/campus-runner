package com.mikasa.campusrunner.common.utils;

import com.mikasa.campusrunner.common.exception.UploadException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageFileInspectorTest {
    @Test
    void inspectsDecodedPngContent() throws IOException {
        byte[] png = imageBytes("png", 12, 8);

        ImageFileInspector.ImageInfo result =
                ImageFileInspector.inspect(png, 100);

        assertEquals("image/png", result.mimeType());
        assertEquals("png", result.extension());
        assertEquals(12, result.width());
        assertEquals(8, result.height());
    }

    @Test
    void rejectsUnsupportedOrSpoofedContent() {
        assertThrows(
                UploadException.class,
                () -> ImageFileInspector.inspect("not-an-image".getBytes(), 100));
    }

    @Test
    void rejectsImageDimensionsAbovePurposeLimit() throws IOException {
        byte[] png = imageBytes("png", 20, 10);

        assertThrows(
                UploadException.class,
                () -> ImageFileInspector.inspect(png, 16));
    }

    @Test
    void readsWebpExtendedHeaderDimensions() {
        byte[] webp = new byte[30];
        writeAscii(webp, 0, "RIFF");
        writeLittleEndian32(webp, 4, 22);
        writeAscii(webp, 8, "WEBP");
        writeAscii(webp, 12, "VP8X");
        writeLittleEndian32(webp, 16, 10);
        writeLittleEndian24(webp, 24, 31);
        writeLittleEndian24(webp, 27, 15);

        ImageFileInspector.ImageInfo result =
                ImageFileInspector.inspect(webp, 100);

        assertEquals("image/webp", result.mimeType());
        assertEquals(32, result.width());
        assertEquals(16, result.height());
    }

    private byte[] imageBytes(
            String format,
            int width,
            int height) throws IOException {
        BufferedImage image =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    private void writeAscii(byte[] target, int offset, String value) {
        for (int i = 0; i < value.length(); i++) {
            target[offset + i] = (byte) value.charAt(i);
        }
    }

    private void writeLittleEndian24(byte[] target, int offset, int value) {
        target[offset] = (byte) (value & 0xff);
        target[offset + 1] = (byte) ((value >> 8) & 0xff);
        target[offset + 2] = (byte) ((value >> 16) & 0xff);
    }

    private void writeLittleEndian32(byte[] target, int offset, int value) {
        target[offset] = (byte) (value & 0xff);
        target[offset + 1] = (byte) ((value >> 8) & 0xff);
        target[offset + 2] = (byte) ((value >> 16) & 0xff);
        target[offset + 3] = (byte) ((value >> 24) & 0xff);
    }
}
