package com.leomall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import com.leomall.admin.annotation.RequiresPermissionsDesc;
import com.leomall.core.util.ResponseUtil;
import com.leomall.core.validator.Order;
import com.leomall.core.validator.Sort;
import com.leomall.db.domain.LeomallUser;
import com.leomall.db.service.LeomallUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/leomall/admin/user")
@Validated
public class AdminUserController {
    private final Log logger = LogFactory.getLog(AdminUserController.class);

    @Autowired
    private LeomallUserService userService;

    @PreAuthorize("hasAuthority('admin:user:list')")
    @RequiresPermissionsDesc(menu = {"用户管理", "会员管理"}, button = "查询")
    @GetMapping("/list")
    public Object list(String username, String mobile,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        List<LeomallUser> userList = userService.querySelective(username, mobile, page, limit, sort, order);
        return ResponseUtil.okList(userList);
    }
    @PreAuthorize("hasAuthority('admin:user:list')")
    @RequiresPermissionsDesc(menu = {"用户管理", "会员管理"}, button = "详情")
    @GetMapping("/detail")
    public Object userDetail(@NotNull Integer id) {
    	LeomallUser user=userService.findById(id);
        return ResponseUtil.ok(user);
    }
    @PreAuthorize("hasAuthority('admin:user:list')")
    @RequiresPermissionsDesc(menu = {"用户管理", "会员管理"}, button = "编辑")
    @PostMapping("/update")
    public Object userUpdate(@RequestBody LeomallUser user) {
        return ResponseUtil.ok(userService.updateById(user));
    }
}
