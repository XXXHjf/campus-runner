package com.mikasa.campusrunner.migration.media;

import com.mikasa.campusrunner.common.properties.AliOSSProperties;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

@Component
public class LegacyMediaUrlNormalizer {
    private static final int MAX_OBJECT_KEY_LENGTH = 191;

    private final AliOSSProperties aliOSSProperties;
    private final HistoricalMediaMigrationProperties migrationProperties;

    public LegacyMediaUrlNormalizer(
            AliOSSProperties aliOSSProperties,
            HistoricalMediaMigrationProperties migrationProperties) {
        this.aliOSSProperties = aliOSSProperties;
        this.migrationProperties = migrationProperties;
    }

    public NormalizedLegacyMedia normalize(String rawUrl) {
        String value = rawUrl == null ? "" : rawUrl.trim();
        String rawHash = sha256("raw:" + value);
        if (value.isEmpty()) {
            return invalid(rawHash, "EMPTY_URL");
        }

        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            return invalid(rawHash, "MALFORMED_URL");
        }
        String scheme = lower(uri.getScheme());
        String host = normalizeHost(uri.getHost());
        if (scheme == null || host == null
                || (!"http".equals(scheme) && !"https".equals(scheme))) {
            return invalid(rawHash, "UNSUPPORTED_URL");
        }

        if (projectOssHosts().contains(host)) {
            String objectKey = uri.getPath();
            if (objectKey != null && objectKey.startsWith("/")) {
                objectKey = objectKey.substring(1);
            }
            String endpointHost = endpointHost(aliOSSProperties.getEndpoint());
            String bucket = lower(aliOSSProperties.getBucketName());
            if (host.equals(endpointHost)
                    && bucket != null
                    && !host.startsWith(bucket + ".")) {
                String bucketPrefix = bucket + "/";
                if (objectKey == null || !objectKey.startsWith(bucketPrefix)) {
                    return invalid(rawHash, "INVALID_OBJECT_KEY");
                }
                objectKey = objectKey.substring(bucketPrefix.length());
            }
            if (!isSafeObjectKey(objectKey)) {
                return invalid(rawHash, "INVALID_OBJECT_KEY");
            }
            return normalizeProjectObjectKey(objectKey, "INVALID_OBJECT_KEY");
        }

        if (!"https".equals(scheme)) {
            return invalid(rawHash, "EXTERNAL_HTTPS_REQUIRED");
        }
        try {
            URI canonical = new URI(
                    "https",
                    null,
                    host,
                    uri.getPort(),
                    emptyPathAsSlash(uri.getRawPath()),
                    uri.getRawQuery(),
                    null);
            return new NormalizedLegacyMedia(
                    LegacyMediaKind.EXTERNAL_HTTPS,
                    sha256(canonical.toASCIIString()),
                    null,
                    canonical,
                    null);
        } catch (URISyntaxException e) {
            return invalid(rawHash, "MALFORMED_URL");
        }
    }

    public String hashForLegacyUrl(String rawUrl) {
        return normalize(rawUrl).urlHash();
    }

    public NormalizedLegacyMedia normalizeDirectAsset(Long assetId, String objectKey) {
        if (!isSafeObjectKey(objectKey)) {
            return invalid(
                    sha256("asset:" + String.valueOf(assetId)),
                    "DIRECT_ASSET_MISSING");
        }
        return normalizeProjectObjectKey(objectKey, "DIRECT_ASSET_MISSING");
    }

    private Set<String> projectOssHosts() {
        Set<String> hosts = new HashSet<>();
        String endpointHost = endpointHost(aliOSSProperties.getEndpoint());
        String bucket = lower(aliOSSProperties.getBucketName());
        if (endpointHost != null) {
            hosts.add(endpointHost);
            if (bucket != null && !endpointHost.startsWith(bucket + ".")) {
                hosts.add(bucket + "." + endpointHost);
            }
        }
        for (String alias : migrationProperties.getProjectOssHostAliases()) {
            String normalized = normalizeHost(alias);
            if (normalized != null) {
                hosts.add(normalized);
            }
        }
        return hosts;
    }

    private String endpointHost(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return null;
        }
        try {
            String value = endpoint.contains("://") ? endpoint : "https://" + endpoint;
            return normalizeHost(URI.create(value).getHost());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isSafeObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()
                || objectKey.length() > MAX_OBJECT_KEY_LENGTH
                || objectKey.startsWith("/")
                || objectKey.contains("\0")) {
            return false;
        }
        for (String segment : objectKey.split("/")) {
            if ("..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    private NormalizedLegacyMedia normalizeProjectObjectKey(
            String objectKey,
            String failureCode) {
        if (!isSafeObjectKey(objectKey)) {
            return invalid(sha256("object:" + String.valueOf(objectKey)), failureCode);
        }
        String canonical = "oss://" + lower(aliOSSProperties.getBucketName())
                + "/" + objectKey;
        return new NormalizedLegacyMedia(
                LegacyMediaKind.PROJECT_OSS,
                sha256(canonical),
                objectKey,
                null,
                null);
    }

    private String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        String value = host.trim();
        if (value.contains("://")) {
            try {
                value = URI.create(value).getHost();
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return IDN.toASCII(value).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String emptyPathAsSlash(String path) {
        return path == null || path.isEmpty() ? "/" : path;
    }

    private String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private NormalizedLegacyMedia invalid(String hash, String code) {
        return new NormalizedLegacyMedia(
                LegacyMediaKind.INVALID,
                hash,
                null,
                null,
                code);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
