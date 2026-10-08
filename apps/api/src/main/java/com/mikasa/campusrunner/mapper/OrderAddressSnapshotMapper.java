package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.AddressBook;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Immutable order evidence; address-book changes never update these rows. */
@Mapper
public interface OrderAddressSnapshotMapper {
    List<Long> lockUsableAddresses(@Param("pickupId") Long pickupId,
            @Param("receiveId") Long receiveId, @Param("userId") Long userId,
            @Param("schoolId") Long schoolId);

    int capture(@Param("orderId") Long orderId, @Param("addressId") Long addressId,
            @Param("role") String role);

    List<Long> findOrderIds(@Param("address") AddressBook address, @Param("role") String role);
}
