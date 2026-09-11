package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.AddressBook;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.io.Resources;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressBookMapperXmlTest {

    @Test
    void updatesOptionalTextFieldsWhenTheyAreExplicitlyCleared() throws IOException {
        AddressBook address = AddressBook.builder()
                .id(1L)
                .details("")
                .label("")
                .build();

        String sql = updateStatement().getBoundSql(address).getSql().replaceAll("\\s+", " ").trim();

        assertTrue(sql.contains("details = ?"));
        assertTrue(sql.contains("label = ?"));
    }

    @Test
    void leavesOptionalTextFieldsUntouchedWhenTheyAreNotProvided() throws IOException {
        AddressBook address = AddressBook.builder()
                .id(1L)
                .isDefault(1)
                .build();

        String sql = updateStatement().getBoundSql(address).getSql().replaceAll("\\s+", " ").trim();

        assertFalse(sql.contains("details = ?"));
        assertFalse(sql.contains("label = ?"));
    }

    private MappedStatement updateStatement() throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/AddressBookMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration.getMappedStatement("com.mikasa.campusrunner.mapper.AddressBookMapper.update");
    }
}
