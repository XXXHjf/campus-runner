package com.mikasa.campusrunner.service.user;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.vo.UserTaskSummaryVO;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class UserTaskService {
    private final UserMapper users;
    private final UserTaskMapper tasks;
    public UserTaskSummaryVO summary() {
        Long owner=BaseContext.getCurrentId();
        if(owner==null) throw new OrderException("登录已失效，请重新登录");
        var user=users.getById(owner);
        if(user==null) throw new OrderException("登录已失效，请重新登录");
        if(!Integer.valueOf(1).equals(user.getAuthentication())) return UserTaskSummaryVO.empty();
        var result=tasks.summarize(owner,user.getSchoolId(),LocalDateTime.now().minusMinutes(30));
        return new UserTaskSummaryVO(result.runnerReceivableCount(),
                Long.valueOf(1).equals(result.runnerReceivableCount())?result.runnerReceivableOrderId():null,
                result.runnerUnpaidCount(),Long.valueOf(1).equals(result.runnerUnpaidCount())?result.runnerUnpaidOrderId():null,
                result.secondHandBuyerCount(),result.secondHandBuyerNoticeCount(),result.secondHandSellerCount(),result.bargainPendingCount());
    }
}
