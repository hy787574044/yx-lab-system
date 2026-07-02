package com.yx.lab.common.security;

import com.yx.lab.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 统一校验控制器声明的接口权限。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;

    @Around("@within(com.yx.lab.common.security.RequirePermission) || @annotation(com.yx.lab.common.security.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (permissionService.isAdmin(currentUser)) {
            return joinPoint.proceed();
        }
        RequirePermission classPermission = joinPoint.getTarget().getClass().getAnnotation(RequirePermission.class);
        RequirePermission methodPermission = resolveMethodPermission(joinPoint);
        checkDeclaredPermission(currentUser, methodPermission == null ? classPermission : methodPermission);
        return joinPoint.proceed();
    }

    private RequirePermission resolveMethodPermission(ProceedingJoinPoint joinPoint) {
        if (!(joinPoint.getSignature() instanceof MethodSignature)) {
            return null;
        }
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return method.getAnnotation(RequirePermission.class);
    }

    private void checkDeclaredPermission(CurrentUser currentUser, RequirePermission requirePermission) {
        if (requirePermission == null || requirePermission.value().length == 0) {
            return;
        }
        boolean allowed = requirePermission.any()
                ? hasAny(currentUser, requirePermission.value())
                : hasAll(currentUser, requirePermission.value());
        if (!allowed) {
            throw new BusinessException("当前用户无权执行该操作");
        }
    }

    private boolean hasAny(CurrentUser currentUser, String[] permissionCodes) {
        for (String permissionCode : permissionCodes) {
            if (permissionService.hasPermission(currentUser, permissionCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAll(CurrentUser currentUser, String[] permissionCodes) {
        for (String permissionCode : permissionCodes) {
            if (!permissionService.hasPermission(currentUser, permissionCode)) {
                return false;
            }
        }
        return true;
    }
}
