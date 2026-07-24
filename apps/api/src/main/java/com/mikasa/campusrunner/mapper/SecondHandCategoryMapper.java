package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.SecondHandCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SecondHandCategoryMapper {
    List<SecondHandCategory> list();

    void insert(SecondHandCategory category);

    void update(SecondHandCategory category);

    void deleteById(@Param("id") Long id);
}
