package com.mikasa.campusrunner.service.user;

import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.pojo.vo.OrderAmountVO;

import java.math.BigDecimal;

public interface OrderAmountService {
    OrderAmountVO calculate(Category category, BigDecimal runnerFee, BigDecimal productAmount);
}
