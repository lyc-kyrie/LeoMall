package com.leomall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import com.leomall.admin.annotation.RequiresPermissionsDesc;
import com.leomall.core.util.ResponseUtil;
import com.leomall.core.validator.Order;
import com.leomall.core.validator.Sort;
import com.leomall.db.domain.LeomallFootprint;
import com.leomall.db.service.LeomallFootprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leomall/admin/footprint")
@Validated
public class AdminFootprintController {
    private final Log logger = LogFactory.getLog(AdminFootprintController.class);

    @Autowired
    private LeomallFootprintService footprintService;

    @PreAuthorize("hasAuthority('admin:footprint:list')")
    @RequiresPermissionsDesc(menu = {"用户管理", "用户足迹"}, button = "查询")
    @GetMapping("/list")
    public Object list(String userId, String goodsId,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        List<LeomallFootprint> footprintList = footprintService.querySelective(userId, goodsId, page, limit, sort,
                order);
        return ResponseUtil.okList(footprintList);
    }
}
