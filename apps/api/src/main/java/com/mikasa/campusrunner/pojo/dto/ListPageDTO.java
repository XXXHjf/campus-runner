package com.mikasa.campusrunner.pojo.dto;

import com.mikasa.campusrunner.common.exception.ParamException;
import lombok.Data;

@Data
public class ListPageDTO {
    private Integer page = 1;
    private Integer pageSize = 20;

    public static int checkedOffset(int page, int pageSize) {
        ListPageDTO query = new ListPageDTO();
        query.setPage(page);
        query.setPageSize(pageSize);
        long offset = query.getOffset();
        if (offset > Integer.MAX_VALUE) throw new ParamException("加载范围有误，请重新尝试");
        return (int) offset;
    }

    public static Integer parseInteger(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value) || "undefined".equalsIgnoreCase(value)) return null;
        try { return Integer.valueOf(value); }
        catch (NumberFormatException error) { throw new ParamException("加载范围有误，请重新尝试"); }
    }

    public int getLimit() {
        int size = pageSize == null ? 20 : pageSize;
        if (size < 1 || size > 50) throw new ParamException("加载范围有误，请重新尝试");
        return size;
    }
    public long getOffset() {
        int index = page == null ? 1 : page;
        if (index < 1) throw new ParamException("加载范围有误，请重新尝试");
        return (index - 1L) * getLimit();
    }
}
