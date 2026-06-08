package com.yx.lab.common.security;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 权限与角色的基础映射服务。
 */
@Service
public class PermissionService {

    public static final String DATA_SCOPE_ALL = "ALL";
    public static final String DATA_SCOPE_SELF = "SELF";
    public static final String DATA_SCOPE_ORG = "ORG";

    public Set<String> resolvePermissions(String roleCode) {
        String normalizedRole = normalizeRole(roleCode);
        if ("ADMIN".equals(normalizedRole) || "DIRECTOR".equals(normalizedRole)) {
            return linkedSet(PermissionConstants.ALL);
        }
        if ("STAFF".equals(normalizedRole)) {
            return linkedSet(
                    PermissionConstants.DASHBOARD_VIEW,
                    PermissionConstants.SAMPLE_VIEW,
                    PermissionConstants.SAMPLE_WRITE,
                    PermissionConstants.SAMPLING_PLAN_VIEW,
                    PermissionConstants.SAMPLING_TASK_VIEW,
                    PermissionConstants.SAMPLING_TASK_WRITE,
                    PermissionConstants.DETECTION_VIEW,
                    PermissionConstants.DETECTION_SUBMIT,
                    PermissionConstants.REPORT_VIEW);
        }
        return linkedSet(PermissionConstants.DASHBOARD_VIEW);
    }

    public String resolveDataScope(String roleCode) {
        String normalizedRole = normalizeRole(roleCode);
        if ("ADMIN".equals(normalizedRole) || "DIRECTOR".equals(normalizedRole)) {
            return DATA_SCOPE_ALL;
        }
        return DATA_SCOPE_SELF;
    }

    public boolean hasPermission(CurrentUser currentUser, String permissionCode) {
        if (currentUser == null || StrUtil.isBlank(permissionCode)) {
            return false;
        }
        Set<String> permissionCodes = currentUser.getPermissionCodes();
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            permissionCodes = resolvePermissions(currentUser.getRoleCode());
        }
        return permissionCodes.contains(PermissionConstants.ALL) || permissionCodes.contains(permissionCode);
    }

    public boolean isAdmin(CurrentUser currentUser) {
        return currentUser != null && hasPermission(currentUser, PermissionConstants.ALL);
    }

    private String normalizeRole(String roleCode) {
        return StrUtil.blankToDefault(StrUtil.trim(roleCode), "").toUpperCase();
    }

    private Set<String> linkedSet(String... values) {
        if (values == null || values.length == 0) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}
