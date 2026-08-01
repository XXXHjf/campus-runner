package com.mikasa.campusrunner.migration.media;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Component
public class ExternalImageDownloader {
    private static final int MAX_REDIRECTS = 3;

    private final HistoricalMediaMigrationProperties properties;
    private final HttpClient httpClient;

    public ExternalImageDownloader(HistoricalMediaMigrationProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getExternalConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public byte[] download(URI initialUri, long maxBytes) {
        URI current = initialUri;
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            validateTarget(current);
            HttpRequest request = HttpRequest.newBuilder(current)
                    .timeout(properties.getExternalReadTimeout())
                    .header("User-Agent", "CampusRunner-HistoricalMediaMigration/1.0")
                    .GET()
                    .build();
            try {
                HttpResponse<InputStream> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    closeQuietly(response.body());
                    String location = response.headers()
                            .firstValue("location")
                            .orElseThrow(() -> new MigrationRecordException(
                                    "EXTERNAL_REDIRECT_INVALID",
                                    "可信外部图片返回了无效跳转"));
                    current = current.resolve(location);
                    continue;
                }
                if (status != 200) {
                    closeQuietly(response.body());
                    throw new MigrationRecordException(
                            "EXTERNAL_HTTP_" + status,
                            "可信外部图片暂时无法访问");
                }
                long declaredLength = response.headers()
                        .firstValueAsLong("content-length")
                        .orElse(-1L);
                if (declaredLength > maxBytes) {
                    closeQuietly(response.body());
                    throw new MigrationRecordException(
                            "IMAGE_TOO_LARGE",
                            "历史图片超过当前大小限制");
                }
                try (InputStream input = response.body()) {
                    byte[] bytes = input.readNBytes(Math.toIntExact(maxBytes + 1));
                    if (bytes.length == 0) {
                        throw new MigrationRecordException(
                                "EXTERNAL_EMPTY_BODY",
                                "可信外部图片返回了空内容");
                    }
                    if (bytes.length > maxBytes) {
                        throw new MigrationRecordException(
                                "IMAGE_TOO_LARGE",
                                "历史图片超过当前大小限制");
                    }
                    return bytes;
                }
            } catch (MigrationRecordException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MigrationRecordException(
                        "EXTERNAL_DOWNLOAD_INTERRUPTED",
                        "可信外部图片下载被中断");
            } catch (IOException | RuntimeException e) {
                throw new MigrationRecordException(
                        "EXTERNAL_DOWNLOAD_FAILED",
                        "可信外部图片暂时无法下载");
            }
        }
        throw new MigrationRecordException(
                "EXTERNAL_REDIRECT_LIMIT",
                "可信外部图片跳转次数过多");
    }

    public boolean isTrusted(URI uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        String host = normalizeHost(uri.getHost());
        if (host == null) {
            return false;
        }
        List<String> allowedHosts = properties.getTrustedExternalHosts();
        return allowedHosts.stream()
                .map(this::normalizeHost)
                .filter(item -> item != null)
                .anyMatch(item -> host.equals(item) || host.endsWith("." + item));
    }

    private void validateTarget(URI uri) {
        if (!isTrusted(uri)) {
            throw new MigrationRecordException(
                    "EXTERNAL_HOST_NOT_TRUSTED",
                    "外部图片域名未列入迁移白名单",
                    true);
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (isBlockedAddress(address)) {
                    throw new MigrationRecordException(
                            "EXTERNAL_ADDRESS_BLOCKED",
                            "外部图片地址不允许访问",
                            true);
                }
            }
        } catch (MigrationRecordException e) {
            throw e;
        } catch (IOException e) {
            throw new MigrationRecordException(
                    "EXTERNAL_DNS_FAILED",
                    "可信外部图片域名无法解析");
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        boolean uniqueLocalV6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
        return uniqueLocalV6
                || address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }

    private String normalizeHost(String host) {
        return host == null || host.isBlank()
                ? null
                : host.trim().toLowerCase(Locale.ROOT);
    }

    private void closeQuietly(InputStream input) {
        try {
            input.close();
        } catch (IOException ignored) {
            // Nothing else can be done for a rejected response body.
        }
    }
}
