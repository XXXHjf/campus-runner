package com.mikasa.campusrunner.mapper;
import com.mikasa.campusrunner.pojo.vo.UserTaskSummaryVO;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
@Mapper
public interface UserTaskMapper {
    UserTaskSummaryVO summarize(@Param("userId") Long userId, @Param("schoolId") Long schoolId,
            @Param("unpaidAfter") LocalDateTime unpaidAfter);
}
