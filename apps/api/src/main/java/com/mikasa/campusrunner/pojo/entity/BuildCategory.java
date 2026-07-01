package com.mikasa.campusrunner.pojo.entity;

import lombok.Data;

/**
 * author  Edith
 * created  2025/1/5 13:35
 */
@Data
public class BuildCategory {
    private Long id;//
    private Long schoolId; //学校id
    private Long compusId; //校区id
    private String name;  //类型名字
    private Long numberId; //校区类型的numberId
    private Integer deleted;      //逻辑删除字段(0 未删除, 1 已删除)
}
