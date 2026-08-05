package com.xiaou.common.aspect;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.StpAdminUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 管理员权限验证切面（基于 Sa-Token）
 * 
 * @author xiaou
 */
@Slf4j
@Aspect
@Component
public class AdminAuthAspect {

    /**
     * 切点：标注了@RequireAdmin注解的方法
     */
    @Pointcut("@annotation(com.xiaou.common.annotation.RequireAdmin)")
    public void adminAuthPointcut() {
    }

    /**
     * 环绕通知：验证管理员权限（基于 Sa-Token）
     */
    @Around("adminAuthPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireAdmin requireAdmin = method.getAnnotation(RequireAdmin.class);
        
        try {
            // 使用 Sa-Token 检查管理员登录
            StpAdminUtil.checkLogin();
            
            // 检查管理员角色
            StpAdminUtil.checkRole("admin");
            
            // 获取当前管理员 ID（用于日志）
            Long adminId = StpAdminUtil.getLoginIdAsLong();
            
            log.debug("管理员权限验证通过，管理员ID: {}, 方法: {}.{}", 
                    adminId,
                    method.getDeclaringClass().getSimpleName(), method.getName());
            
        } catch (NotLoginException e) {
            // Sa-Token 未登录异常
            log.warn("访问需要管理员权限的接口但未登录，方法: {}.{}", 
                    method.getDeclaringClass().getSimpleName(), method.getName());
            throw e;
        } catch (NotRoleException e) {
            // Sa-Token 角色权限异常
            log.warn("普通用户尝试访问管理员接口，方法: {}.{}", 
                    method.getDeclaringClass().getSimpleName(), method.getName());
            throw e;
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            log.error("管理员权限验证异常，方法: {}.{}", 
                    method.getDeclaringClass().getSimpleName(), method.getName(), e);
            throw new BusinessException("权限验证失败");
        }

        // 验证通过后再执行业务方法，业务异常交给全局异常处理器按原语义返回。
        return joinPoint.proceed();
    }
} 
