package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.dto.SecondHandProductQueryDTO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecondHandProductMapperXmlTest {

    @Test
    void filtersByThePickupLocationSavedWhenTheProductWasPublished() throws IOException {
        SecondHandProductQueryDTO query = new SecondHandProductQueryDTO();
        query.setCategoryId(2L);
        query.setPickupAddressPrefix("大学城校区 宿舍 学生公寓");

        String sql = listStatement().getBoundSql(parameters(query)).getSql()
                .replaceAll("\\s+", " ")
                .trim();

        assertTrue(sql.contains("p.category_id = ?"));
        assertTrue(sql.contains("p.pickup_address_snapshot = ?"));
        assertTrue(sql.contains("p.pickup_address_snapshot like concat(?, ' %')"));
    }

    @Test
    void doesNotAddPickupLocationConditionWhenNoLocationWasSelected() throws IOException {
        SecondHandProductQueryDTO query = new SecondHandProductQueryDTO();

        String sql = listStatement().getBoundSql(parameters(query)).getSql()
                .replaceAll("\\s+", " ")
                .trim();

        assertFalse(sql.contains("p.pickup_address_snapshot = ?"));
        assertFalse(sql.contains("p.pickup_address_snapshot like concat(?, ' %')"));
    }

    @Test
    void productDetailIncludesCurrentUsersFavoriteState() throws IOException {
        String sql = statement("detail")
                .getBoundSql(Map.of("id", 10L, "userId", 100L))
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();

        assertTrue(sql.contains("from tb_second_hand_favorite f"));
        assertTrue(sql.contains("f.user_id = ? and f.product_id = p.id"));
        assertTrue(sql.contains("as favorited"));
    }

    @Test
    void adminProductDetailDoesNotBindAUserFavoriteLookup() throws IOException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 10L);
        parameters.put("userId", null);
        String sql = statement("detail")
                .getBoundSql(parameters)
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();

        assertTrue(sql.contains("false as favorited"));
        assertFalse(sql.contains("tb_second_hand_favorite"));
    }

    @Test
    void favoriteListKeepsUnavailableProductsAndUsesFavoriteTimeOrder() throws IOException {
        String sql = statement("listFavorites")
                .getBoundSql(Map.of("userId", 100L))
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();

        assertTrue(sql.contains("inner join tb_second_hand_product p on f.product_id = p.id"));
        assertTrue(sql.contains("p.deleted = 0"));
        assertFalse(sql.contains("p.status = 0"));
        assertTrue(sql.contains("order by f.create_time desc, f.id desc"));
    }

    @Test
    void favoriteCountCannotBecomeNegative() throws IOException {
        String sql = statement("changeFavoriteCount")
                .getBoundSql(Map.of("id", 10L, "delta", -1))
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();

        assertTrue(sql.contains("greatest(coalesce(favorite_count, 0) + ?, 0)"));
    }

    private Map<String, Object> parameters(SecondHandProductQueryDTO query) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("query", query);
        parameters.put("schoolId", 1L);
        return parameters;
    }

    private MappedStatement listStatement() throws IOException {
        return statement("list");
    }

    private MappedStatement statement(String id) throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/SecondHandProductMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandProductMapper." + id);
    }
}
