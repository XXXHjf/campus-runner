package com.mikasa.campusrunner.controller.user;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.dto.MessageDeliveredDTO;
import com.mikasa.campusrunner.pojo.dto.MessageTakeOrderDTO;
import com.mikasa.campusrunner.service.user.MessageSendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * author  Edith
 * created  2024/11/6 14:21
 */
@RestController
@RequestMapping("/api/message")
@Tag(name = "小程序消息发送相关接口")
@Slf4j
public class MessageSendController {






    @PostMapping("/alreadyTakeOrder")
    @Operation(summary = "发送订单已接单通知")
    public Result<String> sendTakeOrder(@RequestBody MessageTakeOrderDTO messageTakeOrderDTO){
        // Compatibility for older clients: state transitions now send the notification.
        return Result.success();
    }

    @PostMapping("/pickUp")
    @Operation(summary = "发送接单人已取货消息")
    public Result<String> sendPickUp(Long orderId){
        // Compatibility for older clients: state transitions now send the notification.
        return Result.success();
    }

    @PostMapping("/delivered")
    @Operation(summary = "发送订单已送达消息")
    public Result<String> sendDelivered(@RequestBody MessageDeliveredDTO messageDeliveredDTO){
        // Compatibility for older clients: state transitions now send the notification.
        return Result.success();
    }

}
