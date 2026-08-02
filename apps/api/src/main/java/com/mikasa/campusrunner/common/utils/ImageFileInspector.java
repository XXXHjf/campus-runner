package com.mikasa.campusrunner.common.utils;

import com.mikasa.campusrunner.common.exception.UploadException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

public final class ImageFileInspector {
    private ImageFileInspector() {
    }

    public static ImageInfo inspect(byte[] bytes, int maxDimension) {
        return inspect(bytes, maxDimension, (long) maxDimension * maxDimension);
    }

    public static ImageInfo inspect(byte[] bytes, int maxDimension, long maxPixels) {
        String mimeType;
        String extension;
        if (isJpeg(bytes)) {
            mimeType = "image/jpeg";
            extension = "jpg";
        } else if (isPng(bytes)) {
            mimeType = "image/png";
            extension = "png";
        } else if (isWebp(bytes)) {
            mimeType = "image/webp";
            extension = "webp";
        } else {
            throw new UploadException("仅支持 JPEG、PNG 或 WebP 图片");
        }
        return inspectWithImageIo(bytes, mimeType, extension, maxDimension, maxPixels);
    }

    private static ImageInfo inspectWithImageIo(
            byte[] bytes,
            String mimeType,
            String extension,
            int maxDimension,
            long maxPixels) {
        try (ImageInputStream input =
                     ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new UploadException("图片内容无法识别");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height, maxDimension, maxPixels);
                if (reader.getNumImages(true) > 1) {
                    throw new UploadException("暂不支持动态图片");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new UploadException("图片内容损坏");
                }
                return new ImageInfo(mimeType, extension, width, height);
            } finally {
                reader.dispose();
            }
        } catch (UploadException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new UploadException("图片内容损坏或格式不受支持");
        }
    }

    private static void validateDimensions(
            int width,
            int height,
            int maxDimension,
            long maxPixels) {
        if (width <= 0 || height <= 0) {
            throw new UploadException("图片尺寸不正确");
        }
        if (width > maxDimension || height > maxDimension) {
            throw new UploadException("图片尺寸过大，请选择较小的图片");
        }
        if ((long) width * height > maxPixels) {
            throw new UploadException("图片像素过高，请选择较小的图片");
        }
    }

    private static boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff;
    }

    private static boolean isPng(byte[] bytes) {
        byte[] signature = new byte[]{
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        };
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWebp(byte[] bytes) {
        return bytes.length >= 16
                && "RIFF".equals(ascii(bytes, 0, 4))
                && "WEBP".equals(ascii(bytes, 8, 4));
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > bytes.length) {
            return "";
        }
        StringBuilder result = new StringBuilder(length);
        for (int i = offset; i < offset + length; i++) {
            result.append((char) (bytes[i] & 0xff));
        }
        return result.toString();
    }

    public record ImageInfo(
            String mimeType,
            String extension,
            int width,
            int height) {
    }
}
