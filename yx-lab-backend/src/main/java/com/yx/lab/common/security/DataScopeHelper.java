package com.yx.lab.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 数据范围判断助手，供各业务查询统一收敛可见数据。
 */
@Component
@RequiredArgsConstructor
public class DataScopeHelper {

    private final PermissionService permissionService;

    public CurrentUser currentUser() {
        return SecurityContext.getCurrentUser();
    }

    public boolean isAdmin() {
        return permissionService.isAdmin(currentUser());
    }

    public boolean isRole(String roleCode) {
        CurrentUser currentUser = currentUser();
        return currentUser != null
                && currentUser.getRoleCode() != null
                && currentUser.getRoleCode().equalsIgnoreCase(roleCode);
    }

    public boolean onlySelfScope() {
        CurrentUser currentUser = currentUser();
        if (currentUser == null || isAdmin()) {
            return false;
        }
        String dataScope = currentUser.getDataScope();
        return dataScope == null || PermissionService.DATA_SCOPE_SELF.equalsIgnoreCase(dataScope);
    }

    public Long currentUserId() {
        CurrentUser currentUser = currentUser();
        return currentUser == null ? null : currentUser.getUserId();
    }

    public Long currentOrgId() {
        CurrentUser currentUser = currentUser();
        return currentUser == null ? null : currentUser.getOrgId();
    }
}
