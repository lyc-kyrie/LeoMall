package com.leomall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.leomall.admin.annotation.RequiresPermissionsDesc;
import com.leomall.core.storage.StorageService;
import com.leomall.core.util.ResponseUtil;
import com.leomall.core.validator.Order;
import com.leomall.core.validator.Sort;
import com.leomall.db.domain.LeomallStorage;
import com.leomall.db.service.LeomallStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/leomall/admin/storage")
@Validated
public class AdminStorageController {
    private final Log logger = LogFactory.getLog(AdminStorageController.class);

    @Autowired
    private StorageService storageService;
    @Autowired
    private LeomallStorageService leomallStorageService;

    @RequiresPermissions("admin:storage:list")
    @RequiresPermissionsDesc(menu = {"系统管理", "对象存储"}, button = "查询")
    @GetMapping("/list")
    public Object list(String key, String name,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        List<LeomallStorage> storageList = leomallStorageService.querySelective(key, name, page, limit, sort, order);
        return ResponseUtil.okList(storageList);
    }

    @RequiresPermissions("admin:storage:create")
    @RequiresPermissionsDesc(menu = {"系统管理", "对象存储"}, button = "上传")
    @PostMapping("/create")
    public Object create(@RequestParam("file") MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        LeomallStorage leomallStorage = storageService.store(file.getInputStream(), file.getSize(),
                file.getContentType(), originalFilename);
        return ResponseUtil.ok(leomallStorage);
    }

    @RequiresPermissions("admin:storage:read")
    @RequiresPermissionsDesc(menu = {"系统管理", "对象存储"}, button = "详情")
    @PostMapping("/read")
    public Object read(@NotNull Integer id) {
        LeomallStorage storageInfo = leomallStorageService.findById(id);
        if (storageInfo == null) {
            return ResponseUtil.badArgumentValue();
        }
        return ResponseUtil.ok(storageInfo);
    }

    @RequiresPermissions("admin:storage:update")
    @RequiresPermissionsDesc(menu = {"系统管理", "对象存储"}, button = "编辑")
    @PostMapping("/update")
    public Object update(@RequestBody LeomallStorage leomallStorage) {
        // P2-8：仅允许修改展示名称。key/url/type/size 由服务端上传时写入，
        // 防止篡改存储记录指向任意路径/类型（配合 fetch 内联回显扩大攻击面）。
        if (leomallStorage.getId() == null) {
            return ResponseUtil.badArgument();
        }
        LeomallStorage existing = leomallStorageService.findById(leomallStorage.getId());
        if (existing == null) {
            return ResponseUtil.badArgumentValue();
        }
        LeomallStorage patch = new LeomallStorage();
        patch.setId(existing.getId());
        patch.setName(leomallStorage.getName());
        if (leomallStorageService.update(patch) == 0) {
            return ResponseUtil.updatedDataFailed();
        }
        existing.setName(patch.getName());
        return ResponseUtil.ok(existing);
    }

    @RequiresPermissions("admin:storage:delete")
    @RequiresPermissionsDesc(menu = {"系统管理", "对象存储"}, button = "删除")
    @PostMapping("/delete")
    public Object delete(@RequestBody LeomallStorage leomallStorage) {
        String key = leomallStorage.getKey();
        if (StringUtils.isEmpty(key)) {
            return ResponseUtil.badArgument();
        }
        leomallStorageService.deleteByKey(key);
        storageService.delete(key);
        return ResponseUtil.ok();
    }
}
