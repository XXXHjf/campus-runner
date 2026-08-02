package com.mikasa.campusrunner.common.utils;

import com.mikasa.campusrunner.common.constant.MediaPurpose;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageProcessingServiceTest {
    private final ImageProcessingService service = new ImageProcessingService();

    @Test
    void normalizesProductImageToPurposePolicy() throws IOException {
        byte[] source = imageBytes("png", 2200, 1400, BufferedImage.TYPE_INT_RGB);

        ImageProcessingService.ProcessedImage result =
                service.process(source, MediaPurpose.SECOND_HAND_PRODUCT_IMAGE);

        assertEquals("image/jpeg", result.mimeType());
        assertEquals("jpg", result.extension());
        assertTrue(result.width() <= 1600);
        assertTrue(result.height() <= 1600);
        assertTrue(result.bytes().length <= MediaPurpose.SECOND_HAND_PRODUCT_IMAGE.getMaxOutputBytes());
    }

    @Test
    void preservesSmallImageDimensionsWhileChangingOutputFormat() throws IOException {
        byte[] source = imageBytes("png", 80, 40, BufferedImage.TYPE_INT_ARGB);

        ImageProcessingService.ProcessedImage result =
                service.process(source, MediaPurpose.AVATAR);

        assertEquals("image/jpeg", result.mimeType());
        assertEquals(80, result.width());
        assertEquals(40, result.height());
    }

    @Test
    void reducesNoisyPngUntilItFitsTheIconLimit() throws IOException {
        BufferedImage image = new BufferedImage(1200, 1200, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(7L);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, random.nextInt());
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        ImageProcessingService.ProcessedImage result =
                service.process(output.toByteArray(), MediaPurpose.ORDER_CATEGORY_ICON);

        assertEquals("image/png", result.mimeType());
        assertTrue(result.width() <= 512);
        assertTrue(result.height() <= 512);
        assertTrue(result.bytes().length <= MediaPurpose.ORDER_CATEGORY_ICON.getMaxOutputBytes());
    }

    private byte[] imageBytes(String format, int width, int height, int type) throws IOException {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(40, 120, 210, type == BufferedImage.TYPE_INT_ARGB ? 160 : 255));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
