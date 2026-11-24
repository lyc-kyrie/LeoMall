package com.leomall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.leomall.admin.annotation.RequiresPermissionsDesc;
import com.leomall.core.util.JacksonUtil;
import com.leomall.core.util.ResponseUtil;
import com.leomall.core.util.RichTextUtil;
import com.leomall.core.validator.Order;
import com.leomall.core.validator.Sort;
import com.leomall.db.domain.*;
import com.leomall.db.service.LeomallAdminService;
import com.leomall.db.service.LeomallNoticeAdminService;
import com.leomall.db.service.LeomallNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.leomall.admin.util.AdminResponseCode.NOTICE_UPDATE_NOT_ALLOWED;

@RestController
@RequestMapping("/leomall/admin/notice")
@Validated
public class AdminNoticeController {
    private final Log logger = LogFactory.getLog(AdminNoticeController.class);

    @Autowired
    private LeomallNoticeService noticeService;
    @Autowired
    private LeomallAdminService adminService;
    @Autowired
    private LeomallNoticeAdminService noticeAdminService;

    @PreAuthorize("hasAuthority('admin:notice:list')")
    @RequiresPermissionsDesc(menu = {"系统管理", "通知管理"}, button = "查询")
    @GetMapping("/list")
    public Object list(String title, String content,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        List<LeomallNotice> noticeList = noticeService.querySelective(title, content, page, limit, sort, order);
        return ResponseUtil.okList(noticeList);
    }

    private Object validate(LeomallNotice notice) {
        String title = notice.getTitle();
        if (StringUtils.isEmpty(title)) {
            return ResponseUtil.badArgument();
        }
        return null;
    }

    private Integer getAdminId(){
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        LeomallAdmin admin = (LeomallAdmin) currentUser.getPrincipal();
        return admin.getId();
    }

    @PreAuthorize("hasAuthority('admin:notice:create')")
    @RequiresPermissionsDesc(menu = {"推广管理", "通知管理"}, button = "添加")
    @PostMapping("/create")
    public Object create(@RequestBody LeomallNotice notice) {
        Object error = validate(notice);
        if (error != null) {
            return error;
        }
        // P2-9：公告内容富文本入库前白名单消毒
        notice.setContent(RichTextUtil.clean(notice.getContent()));
        // 1. 添加通知记录
        notice.setAdminId(getAdminId());
        noticeService.add(notice);
        // 2. 添加管理员通知记录
        List<LeomallAdmin> adminList = adminService.all();
        LeomallNoticeAdmin noticeAdmin = new LeomallNoticeAdmin();
        noticeAdmin.setNoticeId(notice.getId());
        noticeAdmin.setNoticeTitle(notice.getTitle());
        for(LeomallAdmin admin : adminList){
            noticeAdmin.setAdminId(admin.getId());
            noticeAdminService.add(noticeAdmin);
        }
        return ResponseUtil.ok(notice);
    }

    @PreAuthorize("hasAuthority('admin:notice:read')")
    @RequiresPermissionsDesc(menu = {"推广管理", "通知管理"}, button = "详情")
    @GetMapping("/read")
    public Object read(@NotNull Integer id) {
        LeomallNotice notice = noticeService.findById(id);
        List<LeomallNoticeAdmin> noticeAdminList = noticeAdminService.queryByNoticeId(id);
        Map<String, Object> data = new HashMap<>(2);
        data.put("notice", notice);
        data.put("noticeAdminList", noticeAdminList);
        return ResponseUtil.ok(data);
    }

    @PreAuthorize("hasAuthority('admin:notice:update')")
    @RequiresPermissionsDesc(menu = {"推广管理", "通知管理"}, button = "编辑")
    @PostMapping("/update")
    public Object update(@RequestBody LeomallNotice notice) {
        Object error = validate(notice);
        if (error != null) {
            return error;
        }
        // P2-9：公告内容富文本入库前白名单消毒
        notice.setContent(RichTextUtil.clean(notice.getContent()));
        LeomallNotice originalNotice = noticeService.findById(notice.getId());
        if (originalNotice == null) {
            return ResponseUtil.badArgument();
        }
        // 如果通知已经有人阅读过，则不支持编辑
        if(noticeAdminService.countReadByNoticeId(notice.getId()) > 0){
            return ResponseUtil.fail(NOTICE_UPDATE_NOT_ALLOWED, "通知已被阅读，不能重新编辑");
        }
        // 1. 更新通知记录
        notice.setAdminId(getAdminId());
        noticeService.updateById(notice);
        // 2. 更新管理员通知记录
        if(!originalNotice.getTitle().equals(notice.getTitle())){
            LeomallNoticeAdmin noticeAdmin = new LeomallNoticeAdmin();
            noticeAdmin.setNoticeTitle(notice.getTitle());
            noticeAdminService.updateByNoticeId(noticeAdmin, notice.getId());
        }
        return ResponseUtil.ok(notice);
    }

    @PreAuthorize("hasAuthority('admin:notice:delete')")
    @RequiresPermissionsDesc(menu = {"推广管理", "通知管理"}, button = "删除")
    @PostMapping("/delete")
    public Object delete(@RequestBody LeomallNotice notice) {
        // 1. 删除通知管理员记录
        noticeAdminService.deleteByNoticeId(notice.getId());
        // 2. 删除通知记录
        noticeService.deleteById(notice.getId());
        return ResponseUtil.ok();
    }

    @PreAuthorize("hasAuthority('admin:notice:batch-delete')")
    @RequiresPermissionsDesc(menu = {"推广管理", "通知管理"}, button = "批量删除")
    @PostMapping("/batch-delete")
    public Object batchDelete(@RequestBody String body) {
        List<Integer> ids = JacksonUtil.parseIntegerList(body, "ids");
        // 1. 删除通知管理员记录
        noticeAdminService.deleteByNoticeIds(ids);
        // 2. 删除通知记录
        noticeService.deleteByIds(ids);
        return ResponseUtil.ok();
    }
}
