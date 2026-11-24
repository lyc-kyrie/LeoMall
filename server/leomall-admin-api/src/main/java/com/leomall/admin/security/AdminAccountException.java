package com.leomall.admin.security;

import org.springframework.security.authentication.DisabledException;

/**
 * 阶段2B：账号状态异常（锁定/删除），携带原 AdminResponseCode 错误码，
 * 供 AdminAuthController 映射为与 Shiro 时代一致的前端语义（601/602）。
 */
public class AdminAccountException extends DisabledException {

    private final int code;

    public AdminAccountException(int code) {
        super("admin account state error");
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}