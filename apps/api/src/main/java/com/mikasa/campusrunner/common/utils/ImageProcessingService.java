package com.mikasa.campusrunner.common.utils;

import com.mikasa.campusrunner.common.constant.MediaPurpose;
import com.mikasa.campusrunner.common.exception.UploadException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ImageProcessingService {
    private static final float MIN_JPEG_QUALITY = 0.70f;
    private static final int MAX_ATTEMPTS = 10;
    private static final double RESIZE_FACTOR = 0.85d;

    public ProcessedImage process(byte[] source, MediaPurpose purpose) {
        ImageFileInspector.ImageInfo input = ImageFileInspector.inspect(
                source,
                purpose.getMaxInputDimension(),
                purpose.getMaxInputPixels());
        if (!purpose.allows(input.mimeType())) {
            throw new UploadException("该用途不支持此图片格式");
        }

        int maxWidth = purpose.getTargetMaxWidth();
        int maxHeight = purpose.getTargetMaxHeight();
        float quality = purpose.getOutputQuality();
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            byte[] output = encode(source, purpose, maxWidth, maxHeight, quality);
            ImageFileInspector.ImageInfo inspected = ImageFileInspector.inspect(
                    output,
                    Math.max(maxWidth, maxHeight),
                    (long) maxWidth * maxHeight);
            if (output.length <= purpose.getMaxOutputBytes()) {
                return new ProcessedImage(
                        output,
                        purpose.getOutputFormat().getMimeType(),
                        purpose.getOutputFormat().getExtension(),
                        inspected.width(),
                        inspected.height());
            }

            if (purpose.getOutputFormat() == MediaPurpose.ImageOutputFormat.JPEG
                    && quality > MIN_JPEG_QUALITY) {
                quality = Math.max(MIN_JPEG_QUALITY, quality - 0.06f);
            } else {
                maxWidth = Math.max(1, (int) Math.floor(maxWidth * RESIZE_FACTOR));
                maxHeight = Math.max(1, (int) Math.floor(maxHeight * RESIZE_FACTOR));
            }
        }
        throw new UploadException("图片处理失败，请选择清晰度稍低的图片后重试");
    }

    private byte[] encode(
            byte[] source,
            MediaPurpose purpose,
            int maxWidth,
            int maxHeight,
            float quality) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BufferedImage oriented = Thumbnails
                    .of(new ByteArrayInputStream(source))
                    .scale(1.0d)
                    .useExifOrientation(true)
                    .asBufferedImage();
            double scale = Math.min(
                    1.0d,
                    Math.min(
                            (double) maxWidth / oriented.getWidth(),
                            (double) maxHeight / oriented.getHeight()));
            BufferedImage resized = scale < 1.0d
                    ? Thumbnails.of(oriented).scale(scale).asBufferedImage()
                    : oriented;
            if (purpose.getOutputFormat() == MediaPurpose.ImageOutputFormat.JPEG) {
                BufferedImage flattened = new BufferedImage(
                        resized.getWidth(),
                        resized.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = flattened.createGraphics();
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, flattened.getWidth(), flattened.getHeight());
                graphics.drawImage(resized, 0, 0, null);
                graphics.dispose();
                Thumbnails.of(flattened)
                        .scale(1.0d)
                        .outputFormat(purpose.getOutputFormat().getImageIoFormat())
                        .outputQuality(quality)
                        .toOutputStream(output);
            } else if (!ImageIO.write(
                    resized,
                    purpose.getOutputFormat().getImageIoFormat(),
                    output)) {
                throw new IOException("No image writer is available");
            }
            return output.toByteArray();
        } catch (IOException | RuntimeException e) {
            throw new UploadException("图片处理失败，请重新选择图片");
        }
    }

    public record ProcessedImage(
            byte[] bytes,
            String mimeType,
            String extension,
            int width,
            int height) {
    }
}
