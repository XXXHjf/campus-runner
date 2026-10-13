package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.*;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.user.MessageSendService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TakeOrderBoundaryTest {
    @Mock OrderMapper orders;
    @Mock TakeOrderMapper takes;
    @Mock UserMapper users;
    @Mock OrderAddressSnapshotMapper snapshots;
    @Mock MessageSendService messages;
    @InjectMocks TakeOrderServiceImpl service;
    @AfterEach void clear() { BaseContext.removeCurrentId(); }
    private void order(long publisher) {
        BaseContext.setCurrentId(3L);
        when(orders.getByIdForUpdate(2L)).thenReturn(Order.builder().id(2L).userId(publisher).status(0).gap(30).build());
    }
    private UserVO validUser() {
        UserVO user=UserVO.builder().id(3L).deleted(0).authentication(1).schoolId(10L).build();
        when(users.lockRunnerIdentity(3L)).thenReturn(user); return user;
    }
    private void noWrites() {
        verify(orders, never()).update(any()); verifyNoInteractions(takes, messages);
    }
    @Test void selfTakeFailsBeforeIdentityOrSchoolQueries() {
        order(3L);
        assertThrows(TakeOrderException.class, () -> service.take(2L));
        verifyNoInteractions(users, snapshots); noWrites();
    }
    @ParameterizedTest @ValueSource(ints={0,1})
    void missingOrCrossSchoolSnapshotCannotBeBypassedByDirectId(int matchedRoles) {
        order(4L); validUser(); when(snapshots.countMatchingSchool(2L,10L)).thenReturn(matchedRoles);
        assertThrows(TakeOrderException.class, () -> service.take(2L)); noWrites();
    }
    @ParameterizedTest @ValueSource(strings={"deleted","unauthenticated","noSchool","invalidSchool"})
    void invalidRunnerCannotTake(String reason) {
        order(4L); UserVO user=validUser();
        switch(reason) {
            case "deleted": user.setDeleted(1); break;
            case "unauthenticated": user.setAuthentication(0); break;
            case "noSchool": user.setSchoolId(null); break;
            default: user.setSchoolId(0L);
        }
        assertThrows(UserException.class, () -> service.take(2L));
        verifyNoInteractions(snapshots); noWrites();
    }
    @Test void missingLoginCannotReadOrChangeOrder() {
        assertThrows(OrderException.class, () -> service.take(2L));
        verifyNoInteractions(orders, users, snapshots, takes, messages);
    }
    @Test void sameSchoolTakeKeepsOrderThenUserLockOrderAndWritesOnce() {
        order(4L); validUser(); when(snapshots.countMatchingSchool(2L,10L)).thenReturn(2);
        when(orders.update(any())).thenReturn(1); when(takes.save(any())).thenReturn(1);
        service.take(2L);
        InOrder sequence=inOrder(orders, users, snapshots, takes);
        sequence.verify(orders).getByIdForUpdate(2L);
        sequence.verify(users).lockRunnerIdentity(3L);
        sequence.verify(snapshots).countMatchingSchool(2L,10L);
        sequence.verify(orders).update(any()); sequence.verify(takes).save(any());
    }
}
