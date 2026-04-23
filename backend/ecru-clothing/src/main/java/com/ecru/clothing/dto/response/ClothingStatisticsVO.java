package com.ecru.clothing.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ClothingStatisticsVO {

    private Overview overview;
    private List<CategoryStat> byCategory;
    private List<ColorStat> byColor;
    private List<FrequencyStat> byFrequency;
    private List<WearTrend> wearTrend;

    @Data
    public static class Overview {
        private Integer totalClothings;
        private Integer totalWornThisPeriod;
        private Long mostWornClothingId;
        private Integer mostWornCount;
    }

    @Data
    public static class CategoryStat {
        private String category;
        private Integer count;
        private Double percentage;
    }

    @Data
    public static class ColorStat {
        private String color;
        private Integer count;
        private Double percentage;
    }

    @Data
    public static class FrequencyStat {
        private Integer level;
        private String label;
        private Integer count;
    }

    @Data
    public static class WearTrend {
        private String date;
        private Integer count;
    }

}
