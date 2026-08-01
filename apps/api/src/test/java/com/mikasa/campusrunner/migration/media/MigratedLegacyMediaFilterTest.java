package com.mikasa.campusrunner.migration.media;

import com.mikasa.campusrunner.common.properties.AliOSSProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigratedLegacyMediaFilterTest {
    @Mock
    private HistoricalMediaMigrationRepository repository;
    @Mock
    private LegacyMediaFallbackMonitor fallbackMonitor;

    private LegacyMediaUrlNormalizer normalizer;
    private MigratedLegacyMediaFilter filter;

    @BeforeEach
    void setUp() {
        AliOSSProperties oss = new AliOSSProperties();
        oss.setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        oss.setBucketName("campus-runner");
        normalizer = new LegacyMediaUrlNormalizer(
                oss,
                new HistoricalMediaMigrationProperties());
        filter = new MigratedLegacyMediaFilter(repository, normalizer, fallbackMonitor);
    }

    @Test
    void filtersMigratedSignedUrlWithoutDependingOnSignature() {
        String migrated =
                "https://campus-runner.oss-cn-hangzhou.aliyuncs.com/product/a.jpg"
                        + "?Expires=old";
        String remaining =
                "https://campus-runner.oss-cn-hangzhou.aliyuncs.com/product/b.jpg"
                        + "?Expires=old";
        when(repository.findSuccessfulHashes(
                "tb_second_hand_product",
                "images",
                7L))
                .thenReturn(Set.of(normalizer.hashForLegacyUrl(
                        "https://campus-runner.oss-cn-hangzhou.aliyuncs.com/product/a.jpg"
                                + "?Expires=new")));

        List<String> result = filter.keepUnmigrated(
                "tb_second_hand_product",
                "images",
                7L,
                migrated + "," + remaining);

        assertEquals(List.of(remaining), result);
    }
}
