package com.leomall.admin.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.leomall.admin.util.PermissionUtil;
import com.leomall.admin.util.AdminResponseCode;
import com.leomall.core.util.bcrypt.BCryptPasswordEncoder;
import com.leomall.db.domain.LeomallAdmin;
import com.leomall.db.service.LeomallAdminService;
import com.leomall.db.service.LeomallPermissionService;
import com.leomall.db.service.LeomallRoleService;

/**
 * 阶段2B：替代 Shiro AdminAuthorizingRealm 的认证 Provider。
 *
 * - principal 直接是 LeomallAdmin（与 Shiro 时代一致，业务代码 cast 不变）；
 * - 权限来自 DB（permissionService.queryByRoleIds），与原 Realm 一致；
 * - authorities = 权限原串（如 admin:admin:read）+ 角色加 ROLE_ 前缀（hasRole 语义）；
 * - 超管权限 "*" 展开为系统全量权限（Shiro WildcardPermission 语义的等价实现），
 *   同时保留 "*" 本身供超管判断；
 * - 账号不存在/密码错 → BadCredentialsException；已删除 → AdminAccountException，
 *   由 AdminAuthController 映射为原前端语义。
 *
 * service 用 @Lazy 注入（与原 Realm 注释一致，避免事务代理失效/循环依赖）。
 */
@Component
public class AdminAuthenticationProvider implements AuthenticationProvider {

    private final LeomallAdminService adminService;
    private final LeomallRoleService roleService;
    private final LeomallPermissionService permissionService;
    private final ApplicationContext context;

    public AdminAuthenticationProvider(@Lazy LeomallAdminService adminService,
                                       @Lazy LeomallRoleService roleService,
                                       @Lazy LeomallPermissionService permissionService,
                                       ApplicationContext context) {
        this.adminService = adminService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.context = context;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        List<LeomallAdmin> adminList = adminService.findAdmin(username);
        Assert.state(adminList.size() < 2, "同一个用户名存在两个账户");
        if (adminList.isEmpty()) {
            throw new BadCredentialsException("admin can not find this user");
        }
        LeomallAdmin admin = adminList.get(0);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(password, admin.getPassword())) {
            throw new BadCredentialsException("admin username or password error");
        }

        if (Boolean.TRUE.equals(admin.getDeleted())) {
            throw new AdminAccountException(AdminResponseCode.ADMIN_INVALID_ACCOUNT);
        }

        Integer[] roleIds = admin.getRoleIds();
        Set<String> roles = roleService.queryByIds(roleIds);
        Set<String> permissions = permissionService.queryByRoleIds(roleIds);

        Set<GrantedAuthority> authorities = new HashSet<>();
        for (String perm : permissions) {
            if ("*".equals(perm)) {
                // 超管：展开全量权限，方法级 hasAuthority 才能命中；保留 "*" 供超管判断
                authorities.add(new SimpleGrantedAuthority("*"));
                for (String systemPerm : PermissionUtil.listPermissionString(
                        PermissionUtil.listPermission(context, "com.leomall.admin"))) {
                    authorities.add(new SimpleGrantedAuthority(systemPerm));
                }
            } else {
                authorities.add(new SimpleGrantedAuthority(perm));
            }
        }
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        return new UsernamePasswordAuthenticationToken(admin, password, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}