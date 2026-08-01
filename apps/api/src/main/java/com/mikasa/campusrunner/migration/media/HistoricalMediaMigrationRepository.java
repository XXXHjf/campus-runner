package com.mikasa.campusrunner.migration.media;

import com.mikasa.campusrunner.pojo.entity.MediaAsset;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class HistoricalMediaMigrationRepository {
    private static final Set<String> SUCCESSFUL_LEDGER_STATUSES =
            Set.of("MIGRATED", "SUPERSEDED");

    private final JdbcTemplate jdbcTemplate;

    public HistoricalMediaMigrationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long resolveAdminOwnerId(Long configuredId) {
        if (configuredId != null) {
            return configuredId;
        }
        return jdbcTemplate.queryForObject("select min(id) from tb_admin", Long.class);
    }

    public List<LegacyMediaRecord> loadLegacyRecords(Long adminOwnerId) {
        List<LegacyMediaRecord> records = new ArrayList<>();
        records.addAll(jdbcTemplate.query("""
                select id, image
                from tb_category
                where image is not null and trim(image) <> ''
                order by id
                """, (rs, rowNum) -> new LegacyMediaRecord(
                LegacyMediaSource.ORDER_CATEGORY,
                rs.getLong("id"),
                0,
                rs.getString("image"),
                adminOwnerId,
                null)));
        records.addAll(jdbcTemplate.query("""
                select id, image, image_asset_id
                from tb_second_hand_category
                where (image is not null and trim(image) <> '')
                   or image_asset_id is not null
                order by id
                """, (rs, rowNum) -> {
            String image = rs.getString("image");
            Long assetId = nullableLong(rs.getObject("image_asset_id"));
            return new LegacyMediaRecord(
                    image == null || image.isBlank()
                            ? LegacyMediaSource.SECOND_HAND_CATEGORY_ASSET
                            : LegacyMediaSource.SECOND_HAND_CATEGORY,
                    rs.getLong("id"),
                    0,
                    image,
                    adminOwnerId,
                    assetId);
        }));

        List<ProductImages> products = jdbcTemplate.query("""
                select id, seller_id, images
                from tb_second_hand_product
                where images is not null and trim(images) <> ''
                order by id
                """, (rs, rowNum) -> new ProductImages(
                rs.getLong("id"),
                nullableLong(rs.getObject("seller_id")),
                rs.getString("images")));
        for (ProductImages product : products) {
            String[] urls = product.images().split(",", -1);
            for (int index = 0; index < urls.length; index++) {
                if (!urls[index].isBlank()) {
                    records.add(new LegacyMediaRecord(
                            LegacyMediaSource.SECOND_HAND_PRODUCT,
                            product.id(),
                            index,
                            urls[index].trim(),
                            product.sellerId(),
                            null));
                }
            }
        }

        records.addAll(loadSingleUrlRecords(
                """
                        select id, user_id, image
                        from tb_orders
                        where image is not null and trim(image) <> ''
                        order by id
                        """,
                LegacyMediaSource.ORDER));
        records.addAll(loadSingleUrlRecords(
                """
                        select id, user_id, image
                        from tb_take_orders
                        where image is not null and trim(image) <> ''
                        order by id
                        """,
                LegacyMediaSource.TAKE_ORDER));

        jdbcTemplate.query("""
                select id, head_img, student_id_card,
                       alipay_payment_code, wechat_payment_code
                from tb_user
                where (head_img is not null and trim(head_img) <> '')
                   or (student_id_card is not null and trim(student_id_card) <> '')
                   or (alipay_payment_code is not null and trim(alipay_payment_code) <> '')
                   or (wechat_payment_code is not null and trim(wechat_payment_code) <> '')
                order by id
                """, rs -> {
            long userId = rs.getLong("id");
            addUserRecord(records, LegacyMediaSource.USER_AVATAR, userId, rs.getString("head_img"));
            addUserRecord(
                    records,
                    LegacyMediaSource.USER_STUDENT_CARD,
                    userId,
                    rs.getString("student_id_card"));
            addUserRecord(
                    records,
                    LegacyMediaSource.USER_ALIPAY_PAYMENT,
                    userId,
                    rs.getString("alipay_payment_code"));
            addUserRecord(
                    records,
                    LegacyMediaSource.USER_WECHAT_PAYMENT,
                    userId,
                    rs.getString("wechat_payment_code"));
        });

        records.addAll(jdbcTemplate.query("""
                select id, img_url, create_by
                from tb_banner
                where img_url is not null and trim(img_url) <> ''
                order by id
                """, (rs, rowNum) -> new LegacyMediaRecord(
                LegacyMediaSource.BANNER,
                rs.getLong("id"),
                0,
                rs.getString("img_url"),
                Optional.ofNullable(nullableLong(rs.getObject("create_by")))
                        .orElse(adminOwnerId),
                null)));
        records.sort(Comparator
                .comparing((LegacyMediaRecord item) -> item.source().ordinal())
                .thenComparing(LegacyMediaRecord::businessId)
                .thenComparingInt(LegacyMediaRecord::imageIndex));
        return records;
    }

    public boolean ledgerTableExists() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = database()
                  and table_name = 'tb_media_migration_ledger'
                """, Integer.class);
        return count != null && count == 1;
    }

    public Optional<MigrationLedgerEntry> findLedger(
            LegacyMediaRecord record,
            String urlHash) {
        List<MigrationLedgerEntry> entries = jdbcTemplate.query("""
                select id, status, media_asset_id, legacy_url_hash,
                       source_object_key, attempt_count
                from tb_media_migration_ledger
                where source_table = ?
                  and source_column = ?
                  and business_id = ?
                  and image_index = ?
                  and legacy_url_hash = ?
                """, ledgerRowMapper(),
                record.source().getSourceTable(),
                record.source().getSourceColumn(),
                record.businessId(),
                record.imageIndex(),
                urlHash);
        return entries.stream().findFirst();
    }

    public void upsertPendingLedger(
            LegacyMediaRecord record,
            NormalizedLegacyMedia normalized) {
        jdbcTemplate.update("""
                insert into tb_media_migration_ledger(
                    source_table, source_column, business_id, image_index,
                    legacy_url_hash, source_kind, source_object_key,
                    status, attempt_count, last_checked_at, create_time, update_time
                ) values(?, ?, ?, ?, ?, ?, ?, 'PENDING', 1, now(), now(), now())
                on duplicate key update
                    source_kind = values(source_kind),
                    source_object_key = values(source_object_key),
                    attempt_count = attempt_count + 1,
                    last_checked_at = now(),
                    update_time = now()
                """,
                record.source().getSourceTable(),
                record.source().getSourceColumn(),
                record.businessId(),
                record.imageIndex(),
                normalized.urlHash(),
                normalized.kind().name(),
                normalized.objectKey());
    }

    public void markLedgerSuccess(
            LegacyMediaRecord record,
            String urlHash,
            String status,
            Long mediaAssetId) {
        jdbcTemplate.update("""
                update tb_media_migration_ledger
                set status = ?,
                    media_asset_id = ?,
                    failure_code = null,
                    failure_reason = null,
                    migrated_at = now(),
                    last_checked_at = now(),
                    update_time = now()
                where source_table = ?
                  and source_column = ?
                  and business_id = ?
                  and image_index = ?
                  and legacy_url_hash = ?
                """,
                status,
                mediaAssetId,
                record.source().getSourceTable(),
                record.source().getSourceColumn(),
                record.businessId(),
                record.imageIndex(),
                urlHash);
    }

    public void markLedgerFailure(
            LegacyMediaRecord record,
            NormalizedLegacyMedia normalized,
            String status,
            String failureCode,
            String failureReason) {
        jdbcTemplate.update("""
                update tb_media_migration_ledger
                set status = ?,
                    media_asset_id = null,
                    failure_code = ?,
                    failure_reason = ?,
                    migrated_at = null,
                    last_checked_at = now(),
                    update_time = now()
                where source_table = ?
                  and source_column = ?
                  and business_id = ?
                  and image_index = ?
                  and legacy_url_hash = ?
                """,
                status,
                failureCode,
                truncate(failureReason, 500),
                record.source().getSourceTable(),
                record.source().getSourceColumn(),
                record.businessId(),
                record.imageIndex(),
                normalized.urlHash());
    }

    public Set<String> findSuccessfulHashes(
            String sourceTable,
            String sourceColumn,
            Long businessId) {
        if (!ledgerTableExists()) {
            return Set.of();
        }
        try {
            return new HashSet<>(jdbcTemplate.queryForList("""
                    select legacy_url_hash
                    from tb_media_migration_ledger
                    where source_table = ?
                      and source_column = ?
                      and business_id = ?
                      and status in ('MIGRATED', 'SUPERSEDED')
                    """, String.class, sourceTable, sourceColumn, businessId));
        } catch (DataAccessException e) {
            return Set.of();
        }
    }

    public long countOtherSuccessfulLedgerRows(Long mediaAssetId, Long ledgerId) {
        Long count = jdbcTemplate.queryForObject("""
                select count(*)
                from tb_media_migration_ledger
                where media_asset_id = ?
                  and status in ('MIGRATED', 'SUPERSEDED')
                  and id <> ?
                """, Long.class, mediaAssetId, ledgerId == null ? -1L : ledgerId);
        return count == null ? 0L : count;
    }

    public Optional<MediaAsset> findMediaById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        List<MediaAsset> assets =
                jdbcTemplate.query("select * from tb_media_asset where id = ?", mediaRowMapper(), id);
        return assets.stream().findFirst();
    }

    public Optional<MediaAsset> findMediaByObjectKey(String objectKey) {
        if (objectKey == null) {
            return Optional.empty();
        }
        List<MediaAsset> assets = jdbcTemplate.query(
                "select * from tb_media_asset where object_key = ?",
                mediaRowMapper(),
                objectKey);
        return assets.stream().findFirst();
    }

    public List<MediaAsset> findBoundAssets(LegacyMediaRecord record) {
        return jdbcTemplate.query("""
                select *
                from tb_media_asset
                where bound_type = ?
                  and bound_id = ?
                  and purpose = ?
                  and status = 'BOUND'
                order by sort_order, id
                """,
                mediaRowMapper(),
                record.source().getBoundType(),
                record.businessId(),
                record.source().getPurpose().name());
    }

    public int nextSortOrder(LegacyMediaRecord record) {
        Integer max = jdbcTemplate.queryForObject("""
                select max(sort_order)
                from tb_media_asset
                where bound_type = ?
                  and bound_id = ?
                  and purpose = ?
                  and status = 'BOUND'
                """,
                Integer.class,
                record.source().getBoundType(),
                record.businessId(),
                record.source().getPurpose().name());
        return max == null ? 0 : max + 1;
    }

    public Long insertBoundAsset(
            LegacyMediaRecord record,
            String objectKey,
            String mimeType,
            long fileSize,
            int width,
            int height,
            int sortOrder) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into tb_media_asset(
                        object_key, purpose, visibility, owner_type, owner_id,
                        status, mime_type, file_size, width, height,
                        bound_type, bound_id, sort_order, bound_at,
                        delete_retry_count, create_time, update_time
                    ) values(?, ?, ?, ?, ?, 'BOUND', ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, objectKey);
            statement.setString(2, record.source().getPurpose().name());
            statement.setString(3, record.source().getPurpose().getVisibility());
            statement.setString(4, record.source().getOwnerType());
            statement.setLong(5, record.ownerId());
            statement.setString(6, mimeType);
            statement.setLong(7, fileSize);
            statement.setInt(8, width);
            statement.setInt(9, height);
            statement.setString(10, record.source().getBoundType());
            statement.setLong(11, record.businessId());
            statement.setInt(12, sortOrder);
            statement.setObject(13, now);
            statement.setObject(14, now);
            statement.setObject(15, now);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Media asset insert did not return an id");
        }
        return key.longValue();
    }

    private List<LegacyMediaRecord> loadSingleUrlRecords(
            String sql,
            LegacyMediaSource source) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> new LegacyMediaRecord(
                source,
                rs.getLong("id"),
                0,
                rs.getString("image"),
                nullableLong(rs.getObject("user_id")),
                null));
    }

    private void addUserRecord(
            List<LegacyMediaRecord> records,
            LegacyMediaSource source,
            long userId,
            String url) {
        if (url != null && !url.isBlank()) {
            records.add(new LegacyMediaRecord(source, userId, 0, url.trim(), userId, null));
        }
    }

    private RowMapper<MigrationLedgerEntry> ledgerRowMapper() {
        return (rs, rowNum) -> new MigrationLedgerEntry(
                rs.getLong("id"),
                rs.getString("status"),
                nullableLong(rs.getObject("media_asset_id")),
                rs.getString("legacy_url_hash"),
                rs.getString("source_object_key"),
                rs.getInt("attempt_count"));
    }

    private RowMapper<MediaAsset> mediaRowMapper() {
        return (rs, rowNum) -> MediaAsset.builder()
                .id(rs.getLong("id"))
                .objectKey(rs.getString("object_key"))
                .purpose(rs.getString("purpose"))
                .visibility(rs.getString("visibility"))
                .ownerType(rs.getString("owner_type"))
                .ownerId(nullableLong(rs.getObject("owner_id")))
                .status(rs.getString("status"))
                .mimeType(rs.getString("mime_type"))
                .fileSize(nullableLong(rs.getObject("file_size")))
                .width(nullableInteger(rs.getObject("width")))
                .height(nullableInteger(rs.getObject("height")))
                .boundType(rs.getString("bound_type"))
                .boundId(nullableLong(rs.getObject("bound_id")))
                .sortOrder(nullableInteger(rs.getObject("sort_order")))
                .boundAt(rs.getObject("bound_at", LocalDateTime.class))
                .build();
    }

    private Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private Integer nullableInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record ProductImages(Long id, Long sellerId, String images) {
    }
}
