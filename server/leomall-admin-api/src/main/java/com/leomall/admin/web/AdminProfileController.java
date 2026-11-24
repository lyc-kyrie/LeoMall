package com.leomall.admin.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import com.leomall.core.util.JacksonUtil;
import com.leomall.core.util.ResponseUtil;
import com.leomall.core.util.bcrypt.BCryptPasswordEncoder;
import com.leomall.core.validator.Order;
import com.leomall.core.validator.Sort;
import com.leomall.db.domain.LeomallAdmin;
import com.leomall.db.domain.LeomallIssue;
import com.leomall.db.domain.LeomallNotice;
import com.leomall.db.domain.LeomallNoticeAdmin;
import com.leomall.db.service.LeomallAdminService;
import com.leomall.db.service.LeomallNoticeAdminService;
import com.leomall.db.service.LeomallNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.leomall.admin.util.AdminResponseCode.ADMIN_INVALID_ACCOUNT;

@RestController
@RequestMapping("/leomall/admin/profile")
@Validated
public class AdminProfileController {
    private final Log logger = LogFactory.getLog(AdminProfileController.class);

    @Autowired
    private LeomallAdminService adminService;
    @Autowired
    private LeomallNoticeService noticeService;
    @Autowired
    private LeomallNoticeAdminService noticeAdminService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/password")
    public Object create(@RequestBody String body) {
        String oldPassword = JacksonUtil.parseString(body, "oldPassword");
        String newPassword = JacksonUtil.parseString(body, "newPassword");
        if (StringUtils.isEmpty(oldPassword)) {
            return ResponseUtil.badArgument();
        }
        if (StringUtils.isEmpty(newPassword)) {
            return ResponseUtil.badArgument();
        }

        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        LeomallAdmin admin = (LeomallAdmin) currentUser.getPrincipal();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(oldPassword, admin.getPassword())) {
            return ResponseUtil.fail(ADMIN_INVALID_ACCOUNT, "账号密码不对");
        }

        String encodedNewPassword = encoder.encode(newPassword);
        admin.setPassword(encodedNewPassword);

        adminService.updateById(admin);
        return ResponseUtil.ok();
    }

    private Integer getAdminId(){
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        LeomallAdmin admin = (LeomallAdmin) currentUser.getPrincipal();
        return admin.getId();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/nnotice")
    public Object nNotice() {
        int count = noticeAdminService.countUnread(getAdminId());
        return ResponseUtil.ok(count);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/lsnotice")
    public Object lsNotice(String title, String type,
                            @RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer limit,
                            @Sort @RequestParam(defaultValue = "add_time") String sort,
                            @Order @RequestParam(defaultValue = "desc") String order) {
        List<LeomallNoticeAdmin> noticeList = noticeAdminService.querySelective(title, type, getAdminId(), page, limit, sort, order);
        return ResponseUtil.okList(noticeList);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/catnotice")
    public Object catNotice(@RequestBody String body) {
        Integer noticeId = JacksonUtil.parseInteger(body, "noticeId");
        if(noticeId == null){
            return ResponseUtil.badArgument();
        }

        LeomallNoticeAdmin noticeAdmin = noticeAdminService.find(noticeId, getAdminId());
        if(noticeAdmin == null){
           return ResponseUtil.badArgumentValue();
        }
        // 更新通知记录中的时间
        noticeAdmin.setReadTime(LocalDateTime.now());
        noticeAdminService.update(noticeAdmin);

        // 返回通知的相关信息
        Map<String, Object> data = new HashMap<>();
        LeomallNotice notice = noticeService.findById(noticeId);
        data.put("title", notice.getTitle());
        data.put("content", notice.getContent());
        data.put("time", notice.getUpdateTime());
        Integer adminId = notice.getAdminId();
        if(adminId.equals(0)){
            data.put("admin", "系统");
        }
        else{
            LeomallAdmin admin = adminService.findById(notice.getAdminId());
            data.put("admin", admin.getUsername());
            data.put("avatar", admin.getAvatar());
        }
        return ResponseUtil.ok(data);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/bcatnotice")
    public Object bcatNotice(@RequestBody String body) {
        List<Integer> ids = JacksonUtil.parseIntegerList(body, "ids");
        noticeAdminService.markReadByIds(ids, getAdminId());
        return ResponseUtil.ok();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/rmnotice")
    public Object rmNotice(@RequestBody String body) {
        Integer id = JacksonUtil.parseInteger(body, "id");
        if(id == null){
            return ResponseUtil.badArgument();
        }
        noticeAdminService.deleteById(id, getAdminId());
        return ResponseUtil.ok();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/brmnotice")
    public Object brmNotice(@RequestBody String body) {
        List<Integer> ids = JacksonUtil.parseIntegerList(body, "ids");
        noticeAdminService.deleteByIds(ids, getAdminId());
        return ResponseUtil.ok();
    }

}
