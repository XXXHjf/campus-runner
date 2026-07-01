package com.mikasa.campusrunner.common.constant;

import lombok.extern.java.Log;

/**
 * author  Edith
 * created  2024/4/22 9:29
 */
public class NumberIdConstant {
    //初始的number_id
    public static final Long INIT_NUMBER_ID = 1000000000000L;

    //学校每次变化的number_id
    public static final Long SCHOOL_NUMBER_ID_PER_ADD = 10000000000L;

    //校区每次变化的number_id
    public static final Long COMPUS_NUMBER_ID_PER_ADD = 10000000L;

    public static final Long BUILD_CATEGORY_NUMBER_ID_PER_ADD = 10000L;

    //楼宇每次变化的number_id
    public static final Long BUILDING_NUMBER_ID_PER_ADD = 1L;
}
