package com.mikasa.campusrunner.aspect;

import com.mikasa.campusrunner.annotation.AutoFill;
import com.mikasa.campusrunner.common.constant.AutoFillConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;

/**
 * author  Edith
 * created  2024/4/20 21:33
 */
@Component
@Aspect
@Slf4j
public class AutoFillAspect {
    @Pointcut("execution(* com.mikasa.campusrunner.mapper.*.*(..)) && @annotation(com.mikasa.campusrunner.annotation.AutoFill)")
    public void autoFill() {
    }

    /**
     * 自动填充用户id, 更新时间, 创建时间
     *
     * @param joinPoint
     */
    @Before("autoFill()")
    public void m1(JoinPoint joinPoint) {
        log.info("Starting auto-fill aspect...");

        //首先获取这是什么数据库操作类型
        //获取方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        //获取参数
        //默认第一个为实体类
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0){
            return;
        }
        Object entriy = args[0];

        //准备参数
        LocalDateTime now = LocalDateTime.now();

        AutoFill autoFill = method.getAnnotation(AutoFill.class);
        if (autoFill.value() == OperationType.INSERT){
            //如果是insert 插入创建时间
            try {
                Method setCreateTime = entriy.getClass().getMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setUpdateTime = entriy.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                //执行
                setCreateTime.invoke(entriy, now);
                setUpdateTime.invoke(entriy, now);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }else if (autoFill.value() == OperationType.UPDATE){
            //如果是update 插入更新时间
            try {
                Method setUpdateTime = entriy.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                //执行
                setUpdateTime.invoke(entriy, now);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
