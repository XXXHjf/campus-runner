package com.mikasa.campusrunner.pojo.dto;

import com.mikasa.campusrunner.common.exception.ParamException;
import lombok.Data;
import java.util.List;
import java.util.Locale;

@Data
public class SearchPageDTO extends ListPageDTO {
    private String keyword;
    @Override public long getOffset() { getSearchKeyword(); return super.getOffset(); }
    public String getSearchKeyword() {
        String text = keyword == null ? "" : keyword.replaceAll(
                "[\\s\u00a0\u1680\u2000-\u200a\u2028\u2029\u202f\u205f\u3000\ufeff]+", "").toLowerCase(Locale.ROOT);
        if (text.length() > 200) throw new ParamException("搜索内容过长，请缩短后重试");
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
    // Match the existing mini-program \s normalization, without requiring MySQL 8 REGEXP_REPLACE.
    public List<String> getWhitespaceCharacters() {
        return java.util.stream.IntStream.concat(java.util.stream.IntStream.of(9,10,11,12,13,32,160,5760),
                java.util.stream.IntStream.concat(java.util.stream.IntStream.rangeClosed(8192,8202),
                        java.util.stream.IntStream.of(8232,8233,8239,8287,12288,65279)))
                .mapToObj(value -> String.valueOf((char) value)).toList();
    }
}
