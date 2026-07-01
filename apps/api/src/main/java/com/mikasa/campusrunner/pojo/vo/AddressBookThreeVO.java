package com.mikasa.campusrunner.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/27 22:13
 */
@Data
@Builder
public class AddressBookThreeVO {
    @Data
    public static class School {
        private Long numberId;
        private String schoolName;
    }

    @Data
    public static class Compus{
        private Long numberId;
        private String compusName;
    }

    @Data
    public static class BuildCategory {
        private Long numberId;
        private String buildCategoryName;
    }

    @Data
    public static class Building {
        private Long numberId;
        private String buildingName;
    }

    private List<School> school;
    private List<Compus> compus;
    private List<BuildCategory> buildCategory;
    private List<Building> building;

}
