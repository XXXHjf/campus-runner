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
        if (isJpeg(bytes)) {
            return inspectWithImageIo(bytes, "image/jpeg", "jpg", maxDimension);
        }
        if (isPng(bytes)) {
            return inspectWithImageIo(bytes, "image/png", "png", maxDimension);
        }
        if (isWebp(bytes)) {
            ImageInfo info = inspectWebp(bytes);
            validateDimensions(info.width(), info.height(), maxDimension);
            return info;
        }
        throw new UploadException("仅支持 JPEG、PNG 或 WebP 图片");
    }

    private static ImageInfo inspectWithImageIo(
            byte[] bytes,
            String mimeType,
            String extension,
            int maxDimension) {
        try (ImageInputStream input =
                     ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new UploadException("图片内容无法识别");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height, maxDimension);
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new UploadException("图片内容损坏");
                }
                return new ImageInfo(mimeType, extension, width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new UploadException("图片内容损坏或格式不受支持");
        }
    }

    private static ImageInfo inspectWebp(byte[] bytes) {
        if (bytes.length < 30) {
            throw new UploadException("WebP 图片内容损坏");
        }
        long riffSize = littleEndian32(bytes, 4);
        long firstChunkSize = littleEndian32(bytes, 16);
        long firstChunkEnd = 20L + firstChunkSize + (firstChunkSize % 2);
        if (riffSize < 22
                || riffSize + 8 > bytes.length
                || firstChunkSize <= 0
                || firstChunkEnd > bytes.length) {
            throw new UploadException("WebP 图片内容损坏");
        }
        String chunkType = ascii(bytes, 12, 4);
        int width;
        int height;
        switch (chunkType) {
            case "VP8X" -> {
                if (firstChunkSize < 10) {
                    throw new UploadException("WebP 图片内容损坏");
                }
                width = 1 + littleEndian24(bytes, 24);
                height = 1 + littleEndian24(bytes, 27);
            }
            case "VP8L" -> {
                if (firstChunkSize < 5 || (bytes[20] & 0xff) != 0x2f) {
                    throw new UploadException("WebP 图片内容损坏");
                }
                int b1 = bytes[21] & 0xff;
                int b2 = bytes[22] & 0xff;
                int b3 = bytes[23] & 0xff;
                int b4 = bytes[24] & 0xff;
                width = 1 + (b1 | ((b2 & 0x3f) << 8));
                height = 1 + ((b2 >> 6) | (b3 << 2) | ((b4 & 0x0f) << 10));
            }
            case "VP8 " -> {
                if (firstChunkSize < 10
                        || (bytes[23] & 0xff) != 0x9d
                        || (bytes[24] & 0xff) != 0x01
                        || (bytes[25] & 0xff) != 0x2a) {
                    throw new UploadException("WebP 图片内容损坏");
                }
                width = littleEndian16(bytes, 26) & 0x3fff;
                height = littleEndian16(bytes, 28) & 0x3fff;
            }
            default -> throw new UploadException("WebP 图片编码不受支持");
        }
        return new ImageInfo("image/webp", "webp", width, height);
    }

    private static void validateDimensions(int width, int height, int maxDimension) {
        if (width <= 0 || height <= 0) {
            throw new UploadException("图片尺寸不正确");
        }
        if (width > maxDimension || height > maxDimension) {
            throw new UploadException("图片宽高不能超过 " + maxDimension + " 像素");
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

    private static int littleEndian16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }

    private static int littleEndian24(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff)
                | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset + 2] & 0xff) << 16);
    }

    private static long littleEndian32(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffL)
                | ((bytes[offset + 1] & 0xffL) << 8)
                | ((bytes[offset + 2] & 0xffL) << 16)
                | ((bytes[offset + 3] & 0xffL) << 24);
    }

    public record ImageInfo(
            String mimeType,
            String extension,
            int width,
            int height) {
    }
}
