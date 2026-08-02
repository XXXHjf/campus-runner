package com.mikasa.campusrunner.common.utils;

import com.mikasa.campusrunner.common.exception.UploadException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        byte[] fakeWebp = "RIFF0000WEBPVP8X".getBytes();
        assertThrows(
                UploadException.class,
                () -> ImageFileInspector.inspect(fakeWebp, 100));
    }

    @Test
    void rejectsImageDimensionsAbovePurposeLimit() throws IOException {
        byte[] png = imageBytes("png", 20, 10);

        assertThrows(
                UploadException.class,
                () -> ImageFileInspector.inspect(png, 16));
    }

    @Test
    void rejectsImagePixelCountAboveLimit() throws IOException {
        byte[] png = imageBytes("png", 20, 10);

        assertThrows(
                UploadException.class,
                () -> ImageFileInspector.inspect(png, 100, 100));
    }

    @Test
    void decodesWebpContentInsteadOfTrustingItsHeader() throws IOException {
        BufferedImage image = new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "webp", output));

        ImageFileInspector.ImageInfo result =
                ImageFileInspector.inspect(output.toByteArray(), 100);

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

}
