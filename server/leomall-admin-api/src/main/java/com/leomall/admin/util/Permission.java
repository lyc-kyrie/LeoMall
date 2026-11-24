package com.leomall.admin.util;

import com.leomall.admin.annotation.RequiresPermissionsDesc;

/**
 * 阶段2B：权限模型不再持有 Shiro 注解，改存权限字符串
 * （从 @PreAuthorize("hasAuthority('...')") 提取），供角色管理 UI 使用。
 */
public class Permission {
    private String permission;
    private RequiresPermissionsDesc requiresPermissionsDesc;
    private String api;

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public RequiresPermissionsDesc getRequiresPermissionsDesc() {
        return requiresPermissionsDesc;
    }

    public void setRequiresPermissionsDesc(RequiresPermissionsDesc requiresPermissionsDesc) {
        this.requiresPermissionsDesc = requiresPermissionsDesc;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }
}