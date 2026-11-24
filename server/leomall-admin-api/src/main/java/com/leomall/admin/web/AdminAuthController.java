package com.leomall.admin.web;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import com.leomall.admin.service.LogHelper;
import com.leomall.admin.security.AdminAccountException;
import com.leomall.admin.security.AdminLoginGuard;
import com.leomall.admin.util.Permission;
import com.leomall.admin.util.PermissionUtil;
import com.leomall.core.util.IpUtil;
import com.leomall.core.util.JacksonUtil;
import com.leomall.core.util.ResponseUtil;
import com.leomall.core.util.bcrypt.BCryptPasswordEncoder;
import com.leomall.db.domain.LeomallAdmin;
import com.leomall.db.service.LeomallAdminService;
import com.leomall.db.service.LeomallPermissionService;
import com.leomall.db.service.LeomallRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.*;

import static com.leomall.admin.util.AdminResponseCode.*;

/**
 * 阶段2B：Shiro Subject.login → AuthenticationManager + Spring Session。
 * 错误码/返回结构/验证码一次性消费/首登强制改密标记与原版完全一致。
 */
@RestController
@RequestMapping("/leomall/admin/auth")
@Validated
public class AdminAuthController {
    private final Log logger = LogFactory.getLog(AdminAuthController.class);

    @Autowired
    private LeomallAdminService adminService;
    @Autowired
    private LeomallRoleService roleService;
    @Autowired
    private LeomallPermissionService permissionService;
    @Autowired
    private LogHelper logHelper;

    @Autowired
    private AuthenticationManager adminAuthenticationManager;

    @Autowired
    private AdminLoginGuard loginGuard;

    @GetMapping("/kaptcha")
    public Object kaptcha(HttpServletRequest request) {
        String kaptcha = doKaptcha(request);
        if (kaptcha != null) {
            return ResponseUtil.ok(kaptcha);
        }
        return ResponseUtil.fail();
    }

    private String doKaptcha(HttpServletRequest request) {
        // 阶段2B：kaptcha（javax.servlet）→ hutool-captcha；会话属性键不变
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(100, 40, 4, 8);
        HttpSession session = request.getSession();
        session.setAttribute("kaptcha", captcha.getCode());
        return captcha.getImageBase64Data();
    }

    /*
     *  { username : value, password : value, code : value }
     */
    @PostMapping("/login")
    public Object login(@RequestBody String body, HttpServletRequest request) {
        String username = JacksonUtil.parseString(body, "username");
        String password = JacksonUtil.parseString(body, "password");
        String code = JacksonUtil.parseString(body, "code");

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return ResponseUtil.badArgument();
        }

        // P0-3：连续失败锁定（5 次失败锁 15 分钟），防在线爆破
        if (loginGuard.isLocked(username)) {
            return ResponseUtil.fail(ADMIN_INVALID_ACCOUNT, "失败次数过多，账号已临时锁定，请 15 分钟后重试");
        }

        // P0-3：恢复验证码校验（一次性使用，验后即毁）
        if (StringUtils.isEmpty(code)) {
            return ResponseUtil.fail(ADMIN_INVALID_KAPTCHA_REQUIRED, "验证码不能空");
        }
        HttpSession session = request.getSession();
        String kaptcha = (String) session.getAttribute("kaptcha");
        if (kaptcha == null || code.compareToIgnoreCase(kaptcha) != 0) {
            return ResponseUtil.fail(ADMIN_INVALID_KAPTCHA, "验证码不正确", doKaptcha(request));
        }
        session.removeAttribute("kaptcha");

        Authentication authentication;
        try {
            authentication = adminAuthenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (AdminAccountException aae) {
            logHelper.logAuthFail("登录", "用户帐号不可用");
            return ResponseUtil.fail(ADMIN_INVALID_ACCOUNT, "用户帐号不可用");
        } catch (BadCredentialsException bce) {
            loginGuard.recordFailure(username);
            logHelper.logAuthFail("登录", "用户帐号或密码不正确");
            return ResponseUtil.fail(ADMIN_INVALID_ACCOUNT, "用户帐号或密码不正确", doKaptcha(request));
        } catch (AuthenticationException ae) {
            loginGuard.recordFailure(username);
            logHelper.logAuthFail("登录", "认证失败");
            return ResponseUtil.fail(ADMIN_INVALID_ACCOUNT, "认证失败", doKaptcha(request));
        }
        loginGuard.recordSuccess(username);

        // 会话固定攻击防护：认证成功后更换会话 ID（验证码会话已存在）
        request.changeSessionId();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // Spring Security 6 显式保存：写入会话，后续请求由 SecurityContextHolderFilter 恢复
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

        LeomallAdmin admin = (LeomallAdmin) authentication.getPrincipal();
        admin.setLastLoginIp(IpUtil.getIpAddr(request));
        admin.setLastLoginTime(LocalDateTime.now());
        adminService.updateById(admin);

        logHelper.logAuthSucceed("登录");

        // P0-3：仍在使用初始弱口令时，标记前端强制跳转修改密码页
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean mustChangePassword = encoder.matches("admin123", admin.getPassword());

        // userInfo
        Map<String, Object> adminInfo = new HashMap<String, Object>();
        adminInfo.put("nickName", admin.getUsername());
        adminInfo.put("avatar", admin.getAvatar());

        Map<Object, Object> result = new HashMap<Object, Object>();
        result.put("token", request.getSession().getId());
        result.put("adminInfo", adminInfo);
        result.put("mustChangePassword", mustChangePassword);
        return ResponseUtil.ok(result);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public Object logout(HttpServletRequest request) {
        logHelper.logAuthSucceed("退出");
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseUtil.ok();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/info")
    public Object info() {
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        LeomallAdmin admin = (LeomallAdmin) currentUser.getPrincipal();

        Map<String, Object> data = new HashMap<>();
        data.put("name", admin.getUsername());
        data.put("avatar", admin.getAvatar());

        Integer[] roleIds = admin.getRoleIds();
        Set<String> roles = roleService.queryByIds(roleIds);
        Set<String> permissions = permissionService.queryByRoleIds(roleIds);
        data.put("roles", roles);
        // NOTE
        // 这里需要转换perms结构，因为对于前端而已API形式的权限更容易理解
        data.put("perms", toApi(permissions));
        return ResponseUtil.ok(data);
    }

    @Autowired
    private ApplicationContext context;
    private HashMap<String, String> systemPermissionsMap = null;

    private Collection<String> toApi(Set<String> permissions) {
        if (systemPermissionsMap == null) {
            systemPermissionsMap = new HashMap<>();
            final String basicPackage = "com.leomall.admin";
            List<Permission> systemPermissions = PermissionUtil.listPermission(context, basicPackage);
            for (Permission permission : systemPermissions) {
                String perm = permission.getPermission();
                String api = permission.getApi();
                systemPermissionsMap.put(perm, api);
            }
        }

        Collection<String> apis = new HashSet<>();
        for (String perm : permissions) {
            String api = systemPermissionsMap.get(perm);
            apis.add(api);

            if (perm.equals("*")) {
                apis.clear();
                apis.add("*");
                return apis;
            }
        }
        return apis;
    }

    @GetMapping("/401")
    public Object page401() {
        return ResponseUtil.unlogin();
    }

    @GetMapping("/index")
    public Object pageIndex() {
        return ResponseUtil.ok();
    }

    @GetMapping("/403")
    public Object page403() {
        return ResponseUtil.unauthz();
    }
}