package com.mikasa.campusrunner.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecondHandFavoriteMapperXmlTest {

    @Test
    void duplicateFavoriteIsAnIdempotentInsert() throws IOException {
        String sql = statement("insertIgnore")
                .getBoundSql(Map.of(
                        "userId", 100L,
                        "productId", 10L,
                        "createTime", LocalDateTime.now()))
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();

        assertTrue(sql.startsWith("insert ignore into tb_second_hand_favorite"));
        assertTrue(sql.contains("user_id, product_id, create_time"));
    }

    @Test
    void cancelFavoriteTargetsOnlyCurrentUsersRelationship() throws IOException {
        String sql = statement("delete")
                .getBoundSql(Map.of("userId", 100L, "productId", 10L))
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();

        assertTrue(sql.contains("where user_id = ? and product_id = ?"));
    }

    private MappedStatement statement(String id) throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/SecondHandFavoriteMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration.getMappedStatement(
                "com.mikasa.campusrunner.mapper.SecondHandFavoriteMapper." + id);
    }
}
