package com.leomall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import com.leomall.admin.annotation.RequiresPermissionsDesc;
import com.leomall.core.util.ResponseUtil;
import com.leomall.core.validator.Order;
import com.leomall.core.validator.Sort;
import com.leomall.db.domain.LeomallFeedback;
import com.leomall.db.service.LeomallFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @date 2018/8/26 1:11
 */
@RestController
@RequestMapping("/leomall/admin/feedback")
@Validated
public class AdminFeedbackController {
    private final Log logger = LogFactory.getLog(AdminFeedbackController.class);

    @Autowired
    private LeomallFeedbackService feedbackService;

    @PreAuthorize("hasAuthority('admin:feedback:list')")
    @RequiresPermissionsDesc(menu = {"用户管理", "意见反馈"}, button = "查询")
    @GetMapping("/list")
    public Object list(Integer userId, String username,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        List<LeomallFeedback> feedbackList = feedbackService.querySelective(userId, username, page, limit, sort,
                order);
        return ResponseUtil.okList(feedbackList);
    }
}
