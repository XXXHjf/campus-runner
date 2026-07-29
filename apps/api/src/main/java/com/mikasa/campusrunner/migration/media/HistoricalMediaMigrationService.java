package com.mikasa.campusrunner.migration.media;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.exception.UploadException;
import com.mikasa.campusrunner.common.utils.AliOSSUtil;
import com.mikasa.campusrunner.common.utils.ImageFileInspector;
import com.mikasa.campusrunner.pojo.entity.MediaAsset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class HistoricalMediaMigrationService {
    private final HistoricalMediaMigrationProperties properties;
    private final HistoricalMediaMigrationRepository repository;
    private final LegacyMediaUrlNormalizer normalizer;
    private final ExternalImageDownloader externalImageDownloader;
    private final AliOSSUtil aliOSSUtil;
    private final TransactionTemplate transactionTemplate;

    public HistoricalMediaMigrationService(
            HistoricalMediaMigrationProperties properties,
            HistoricalMediaMigrationRepository repository,
            LegacyMediaUrlNormalizer normalizer,
            ExternalImageDownloader externalImageDownloader,
            AliOSSUtil aliOSSUtil,
            TransactionTemplate transactionTemplate) {
        this.properties = properties;
        this.repository = repository;
        this.normalizer = normalizer;
        this.externalImageDownloader = externalImageDownloader;
        this.aliOSSUtil = aliOSSUtil;
        this.transactionTemplate = transactionTemplate;
    }

    public HistoricalMediaMigrationSummary execute(MigrationMode mode) {
        if (mode == MigrationMode.APPLY && !properties.isAllowWrites()) {
            throw new IllegalStateException(
                    "APPLY mode requires media-migration.allow-writes=true");
        }
        if (mode != MigrationMode.DRY_RUN && !repository.ledgerTableExists()) {
            throw new IllegalStateException(
                    "tb_media_migration_ledger is missing; apply the history migration schema first");
        }

        Long adminOwnerId = repository.resolveAdminOwnerId(properties.getAdminOwnerId());
        List<LegacyMediaRecord> records = applyLimit(repository.loadLegacyRecords(adminOwnerId));
        HistoricalMediaMigrationSummary summary = new HistoricalMediaMigrationSummary(mode);
        summary.add("records.total", records.size());
        Set<String> dryRunSeenProjectObjects = new HashSet<>();

        for (LegacyMediaRecord record : records) {
            NormalizedLegacyMedia normalized = normalizeRecord(record);
            summary.increment("source." + record.source().name());
            summary.increment("kind." + normalized.kind().name());
            if (mode == MigrationMode.VERIFY) {
                verifyRecord(record, normalized, summary);
                continue;
            }
            if (mode == MigrationMode.APPLY) {
                repository.upsertPendingLedger(record, normalized);
            }
            try {
                if (mode == MigrationMode.APPLY
                        && reuseCompletedLedger(record, normalized, summary)) {
                    continue;
                }
                validateRecordIdentity(record, normalized);
                ValidatedImage sourceImage = fetchAndValidate(record, normalized);
                if (mode == MigrationMode.DRY_RUN) {
                    dryRunRecord(
                            record,
                            normalized,
                            sourceImage,
                            dryRunSeenProjectObjects,
                            summary);
                } else {
                    applyRecord(record, normalized, sourceImage, summary);
                }
            } catch (MigrationRecordException e) {
                recordFailure(mode, record, normalized, e, summary);
            } catch (RuntimeException e) {
                MigrationRecordException wrapped = new MigrationRecordException(
                        "UNEXPECTED_RECORD_FAILURE",
                        safeMessage(e));
                recordFailure(mode, record, normalized, wrapped, summary);
            }
        }
        return summary;
    }

    private NormalizedLegacyMedia normalizeRecord(LegacyMediaRecord record) {
        if (record.legacyUrl() != null && !record.legacyUrl().isBlank()) {
            return normalizer.normalize(record.legacyUrl());
        }
        String objectKey = repository.findMediaById(record.directAssetId())
                .map(MediaAsset::getObjectKey)
                .orElse(null);
        return normalizer.normalizeDirectAsset(record.directAssetId(), objectKey);
    }

    private void dryRunRecord(
            LegacyMediaRecord record,
            NormalizedLegacyMedia normalized,
            ValidatedImage sourceImage,
            Set<String> seenProjectObjects,
            HistoricalMediaMigrationSummary summary) {
        List<MediaAsset> boundAssets = repository.findBoundAssets(record);
        Optional<MediaAsset> matching = findMatchingBoundAsset(boundAssets, normalized);
        if (matching.isPresent() && verifyAsset(record, matching.get(), false)) {
            summary.increment("dry_run.already_migrated");
            return;
        }
        if (record.source().getMaxCount() == 1 && !boundAssets.isEmpty()) {
            requireValidExistingBinding(record, boundAssets.get(0));
            summary.increment("dry_run.would_supersede_legacy");
            return;
        }
        if (normalized.kind() == LegacyMediaKind.EXTERNAL_HTTPS) {
            summary.increment("dry_run.would_upload_external");
            return;
        }
        boolean repeated = !seenProjectObjects.add(normalized.objectKey());
        Optional<MediaAsset> objectAsset =
                repository.findMediaByObjectKey(normalized.objectKey());
        if (repeated || objectAsset.isPresent()) {
            summary.increment("dry_run.would_copy_shared_object");
        } else {
            summary.increment("dry_run.would_register_existing_object");
        }
        summary.add("dry_run.validated_bytes", sourceImage.bytes().length);
    }

    private void applyRecord(
            LegacyMediaRecord record,
            NormalizedLegacyMedia normalized,
            ValidatedImage sourceImage,
            HistoricalMediaMigrationSummary summary) {
        Optional<MigrationLedgerEntry> existingLedger =
                repository.findLedger(record, normalized.urlHash());

        List<MediaAsset> boundAssets = repository.findBoundAssets(record);
        Optional<MediaAsset> matching = findMatchingBoundAsset(boundAssets, normalized);
        if (matching.isPresent()) {
            long otherLedgerUses = repository.countOtherSuccessfulLedgerRows(
                    matching.get().getId(),
                    existingLedger.map(MigrationLedgerEntry::id).orElse(null));
            if (otherLedgerUses == 0 && verifyAsset(record, matching.get(), false)) {
                repository.markLedgerSuccess(
                        record,
                        normalized.urlHash(),
                        "MIGRATED",
                        matching.get().getId());
                summary.increment("apply.registered_existing_binding");
                return;
            }
        }

        if (record.source().getMaxCount() == 1 && !boundAssets.isEmpty()) {
            MediaAsset current = boundAssets.get(0);
            requireValidExistingBinding(record, current);
            repository.markLedgerSuccess(
                    record,
                    normalized.urlHash(),
                    "SUPERSEDED",
                    current.getId());
            summary.increment("apply.superseded_legacy");
            return;
        }

        String targetObjectKey = chooseTargetObjectKey(record, normalized, sourceImage);
        if (!Objects.equals(targetObjectKey, normalized.objectKey())) {
            ensureIndependentObject(targetObjectKey, sourceImage);
            summary.increment("apply.copied_object");
        } else {
            summary.increment("apply.reused_project_object");
        }

        Long mediaAssetId = transactionTemplate.execute(status -> {
            Optional<MediaAsset> concurrent =
                    repository.findMediaByObjectKey(targetObjectKey);
            if (concurrent.isPresent()) {
                if (!isExpectedBinding(record, concurrent.get())) {
                    throw new MigrationRecordException(
                            "OBJECT_KEY_ALREADY_BOUND",
                            "迁移目标对象已被其他业务占用",
                            true);
                }
                repository.markLedgerSuccess(
                        record,
                        normalized.urlHash(),
                        "MIGRATED",
                        concurrent.get().getId());
                return concurrent.get().getId();
            }
            int sortOrder = record.source().getMaxCount() == 1
                    ? 0
                    : repository.nextSortOrder(record);
            Long id = repository.insertBoundAsset(
                    record,
                    targetObjectKey,
                    sourceImage.info().mimeType(),
                    sourceImage.bytes().length,
                    sourceImage.info().width(),
                    sourceImage.info().height(),
                    sortOrder);
            repository.markLedgerSuccess(
                    record,
                    normalized.urlHash(),
                    "MIGRATED",
                    id);
            return id;
        });
        if (mediaAssetId == null) {
            throw new MigrationRecordException(
                    "DATABASE_TRANSACTION_FAILED",
                    "迁移资源登记事务未完成");
        }
        summary.increment("apply.migrated");
    }

    private boolean reuseCompletedLedger(
            LegacyMediaRecord record,
            NormalizedLegacyMedia normalized,
            HistoricalMediaMigrationSummary summary) {
        Optional<MigrationLedgerEntry> ledger =
                repository.findLedger(record, normalized.urlHash());
        if (ledger.isEmpty() || !ledger.get().isSuccessful()) {
            return false;
        }
        Optional<MediaAsset> asset = repository.findMediaById(ledger.get().mediaAssetId());
        if (asset.isEmpty() || !verifyAsset(record, asset.get(), true)) {
            return false;
        }
        summary.increment("apply.already_completed");
        return true;
    }

    private void verifyRecord(
            LegacyMediaRecord record,
            NormalizedLegacyMedia normalized,
            HistoricalMediaMigrationSummary summary) {
        Optional<MigrationLedgerEntry> ledger =
                repository.findLedger(record, normalized.urlHash());
        if (ledger.isEmpty()) {
            summary.increment("verify.untracked");
            return;
        }
        if (!ledger.get().isSuccessful()) {
            summary.increment("verify.explicit_" + ledger.get().status().toLowerCase());
            return;
        }
        Optional<MediaAsset> asset = repository.findMediaById(ledger.get().mediaAssetId());
        if (asset.isEmpty()) {
            summary.increment("verify.missing_media_row");
            return;
        }
        try {
            requireValidExistingBinding(record, asset.get());
            summary.increment("verify.ok");
        } catch (MigrationRecordException e) {
            summary.increment("verify.failure." + e.getCode());
        }
    }

    private void validateRecordIdentity(
            LegacyMediaRecord record,
            NormalizedLegacyMedia normalized) {
        if (!normalized.isValid()) {
            throw new MigrationRecordException(
                    normalized.failureCode(),
                    "历史图片地址无法自动识别",
                    normalized.failureCode().contains("HTTPS"));
        }
        if (record.businessId() == null || record.ownerId() == null) {
            throw new MigrationRecordException(
                    "OWNER_NOT_FOUND",
                    "历史图片找不到可用的资源所有者",
                    true);
        }
        if (normalized.kind() == LegacyMediaKind.EXTERNAL_HTTPS
                && !externalImageDownloader.isTrusted(normalized.externalUri())) {
            throw new MigrationRecordException(
                    "EXTERNAL_HOST_NOT_TRUSTED",
                    "外部图片域名未列入迁移白名单",
                    true);
        }
    }

    private ValidatedImage fetchAndValidate(
            LegacyMediaRecord record,
            NormalizedLegacyMedia normalized) {
        byte[] bytes;
        try {
            if (normalized.kind() == LegacyMediaKind.PROJECT_OSS) {
                if (!aliOSSUtil.objectExists(normalized.objectKey())) {
                    throw new MigrationRecordException(
                            "OSS_OBJECT_MISSING",
                            "历史图片对应的 OSS 对象不存在");
                }
                bytes = aliOSSUtil.downloadObject(
                        normalized.objectKey(),
                        record.source().getPurpose().getMaxBytes());
            } else {
                bytes = externalImageDownloader.download(
                        normalized.externalUri(),
                        record.source().getPurpose().getMaxBytes());
            }
        } catch (MigrationRecordException e) {
            throw e;
        } catch (UploadException e) {
            String code = e.getMessage() != null
                    && e.getMessage().contains("大小不符合")
                    ? "IMAGE_SIZE_INVALID"
                    : "OSS_READ_FAILED";
            throw new MigrationRecordException(code, safeMessage(e));
        }
        try {
            ImageFileInspector.ImageInfo info = ImageFileInspector.inspect(
                    bytes,
                    record.source().getPurpose().getMaxDimension());
            if (!record.source().getPurpose().allows(info.mimeType())) {
                throw new MigrationRecordException(
                        "IMAGE_MIME_NOT_ALLOWED",
                        "历史图片格式不符合当前用途限制");
            }
            return new ValidatedImage(bytes, info);
        } catch (MigrationRecordException e) {
            throw e;
        } catch (UploadException e) {
            throw new MigrationRecordException(
                    "IMAGE_FORMAT_INVALID",
                    safeMessage(e));
        }
    }

    private Optional<MediaAsset> findMatchingBoundAsset(
            List<MediaAsset> assets,
            NormalizedLegacyMedia normalized) {
        if (normalized.kind() != LegacyMediaKind.PROJECT_OSS) {
            return Optional.empty();
        }
        return assets.stream()
                .filter(asset -> normalized.objectKey().equals(asset.getObjectKey()))
                .findFirst();
    }

    private String chooseTargetObjectKey(
            LegacyMediaRecord record,
            NormalizedLegacyMedia normalized,
            ValidatedImage sourceImage) {
        if (normalized.kind() == LegacyMediaKind.PROJECT_OSS
                && repository.findMediaByObjectKey(normalized.objectKey()).isEmpty()) {
            return normalized.objectKey();
        }
        String sourceName = record.source().name().toLowerCase();
        return "media/" + record.source().getPurpose().getObjectPrefix()
                + "/migration/" + sourceName
                + "/" + record.businessId()
                + "/" + record.imageIndex()
                + "-" + normalized.urlHash().substring(0, 16)
                + "." + sourceImage.info().extension();
    }

    private void ensureIndependentObject(String targetObjectKey, ValidatedImage sourceImage) {
        if (!aliOSSUtil.objectExists(targetObjectKey)) {
            aliOSSUtil.uploadObject(targetObjectKey, sourceImage.bytes());
            return;
        }
        byte[] existing = aliOSSUtil.downloadObject(
                targetObjectKey,
                sourceImage.bytes().length);
        if (!MessageDigest.isEqual(sha256(existing), sha256(sourceImage.bytes()))) {
            throw new MigrationRecordException(
                    "MIGRATION_OBJECT_COLLISION",
                    "迁移目标对象与当前历史图片内容不一致",
                    true);
        }
    }

    private void requireValidExistingBinding(
            LegacyMediaRecord record,
            MediaAsset asset) {
        if (!isExpectedBinding(record, asset)) {
            throw new MigrationRecordException(
                    "EXISTING_BINDING_INVALID",
                    "现有资源绑定信息与业务记录不一致",
                    true);
        }
        if (!aliOSSUtil.objectExists(asset.getObjectKey())) {
            throw new MigrationRecordException(
                    "EXISTING_BOUND_OBJECT_MISSING",
                    "现有资源绑定对应的 OSS 对象不存在",
                    true);
        }
        try {
            byte[] bytes = aliOSSUtil.downloadObject(
                    asset.getObjectKey(),
                    record.source().getPurpose().getMaxBytes());
            ImageFileInspector.inspect(
                    bytes,
                    record.source().getPurpose().getMaxDimension());
        } catch (UploadException e) {
            throw new MigrationRecordException(
                    "EXISTING_BOUND_OBJECT_INVALID",
                    "现有资源绑定对应的图片无法通过校验",
                    true);
        }
    }

    private boolean verifyAsset(
            LegacyMediaRecord record,
            MediaAsset asset,
            boolean requireObjectValidation) {
        if (!isExpectedBinding(record, asset)) {
            return false;
        }
        if (requireObjectValidation) {
            requireValidExistingBinding(record, asset);
        }
        return true;
    }

    private boolean isExpectedBinding(LegacyMediaRecord record, MediaAsset asset) {
        return MediaAssetConstant.STATUS_BOUND.equals(asset.getStatus())
                && record.source().getPurpose().name().equals(asset.getPurpose())
                && record.source().getPurpose().getVisibility().equals(asset.getVisibility())
                && record.source().getBoundType().equals(asset.getBoundType())
                && record.businessId().equals(asset.getBoundId());
    }

    private void recordFailure(
            MigrationMode mode,
            LegacyMediaRecord record,
            NormalizedLegacyMedia normalized,
            MigrationRecordException error,
            HistoricalMediaMigrationSummary summary) {
        String status = error.isManualReview() ? "MANUAL_REQUIRED" : "FAILED";
        summary.increment(mode.name().toLowerCase()
                + "." + status.toLowerCase()
                + "." + error.getCode());
        summary.increment("source." + record.source().name()
                + "." + status.toLowerCase()
                + "." + error.getCode());
        log.warn(
                "Historical media record needs attention: source={}.{}, businessId={}, "
                        + "imageIndex={}, status={}, code={}",
                record.source().getSourceTable(),
                record.source().getSourceColumn(),
                record.businessId(),
                record.imageIndex(),
                status,
                error.getCode());
        if (mode == MigrationMode.APPLY) {
            repository.markLedgerFailure(
                    record,
                    normalized,
                    status,
                    error.getCode(),
                    safeMessage(error));
        }
    }

    private List<LegacyMediaRecord> applyLimit(List<LegacyMediaRecord> records) {
        int max = properties.getMaxRecords();
        if (max <= 0 || records.size() <= max) {
            return records;
        }
        return new ArrayList<>(records.subList(0, max));
    }

    private byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private record ValidatedImage(
            byte[] bytes,
            ImageFileInspector.ImageInfo info) {
    }
}
