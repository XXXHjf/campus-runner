package com.mikasa.campusrunner.pojo.dto;
import com.mikasa.campusrunner.common.exception.ParamException;
import lombok.Data;
import java.util.List;
@Data
public class PublicOrderQueryDTO extends ListPageDTO {
    private Long categoryId;
    private String sort = "time";
    private String keyword;
    private String pickupLabels;
    private String receiveLabels;
    @Override public long getOffset() {
        if (sort == null) sort = "time";
        if (!validSort(sort) || (categoryId != null && categoryId <= 0)) {
            throw new ParamException("筛选条件有误，请重新选择");
        }
        getLiteralKeyword();getPickupFilters();getReceiveFilters();return super.getOffset();
    }
    protected boolean validSort(String value) { return java.util.Set.of("time","price_desc","price_asc").contains(value); }
    public String getLiteralKeyword() {
        String text = keyword == null ? "" : keyword.replaceAll("^[\\s\u00a0\u1680\u2000-\u200a\u2028\u2029\u202f\u205f\u3000\ufeff]+|[\\s\u00a0\u1680\u2000-\u200a\u2028\u2029\u202f\u205f\u3000\ufeff]+$", "").toLowerCase(java.util.Locale.ROOT);
        if (text.length()>200) throw new ParamException("搜索内容过长，请缩短后重试");
        return text.replace("!","!!").replace("%","!%").replace("_","!_");
    }
    public List<String> getPickupFilters() { return labels(pickupLabels); }
    public List<String> getReceiveFilters() { return labels(receiveLabels); }
    private List<String> labels(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(value);
            if (!json.isArray() || json.size()>4) throw new IllegalArgumentException();
            var result = new java.util.ArrayList<String>();
            for(var label:json) {
                if(!label.isTextual() || label.textValue().length()>100) throw new IllegalArgumentException();
                if(!label.textValue().isEmpty())result.add(label.textValue());
            }
            return result;
        } catch(Exception error) { throw new ParamException("地址筛选有误，请重新选择"); }
    }
}
