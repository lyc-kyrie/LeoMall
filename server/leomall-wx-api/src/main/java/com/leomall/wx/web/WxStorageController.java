package com.leomall.wx.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.leomall.core.storage.StorageService;
import com.leomall.core.util.CharUtil;
import com.leomall.core.util.ResponseUtil;
import com.leomall.db.domain.LeomallStorage;
import com.leomall.db.service.LeomallStorageService;
import com.leomall.wx.annotation.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 对象存储服务
 */
@RestController
@RequestMapping("/leomall/wx/storage")
@Validated
public class WxStorageController {
    private final Log logger = LogFactory.getLog(WxStorageController.class);

    @Autowired
    private StorageService storageService;
    @Autowired
    private LeomallStorageService leomallStorageService;

    private String generateKey(String originalFilename) {
        int index = originalFilename.lastIndexOf('.');
        String suffix = originalFilename.substring(index);

        String key = null;
        LeomallStorage storageInfo = null;

        do {
            key = CharUtil.getRandomString(20) + suffix;
            storageInfo = leomallStorageService.findByKey(key);
        }
        while (storageInfo != null);

        return key;
    }

    @PostMapping("/upload")
    public Object upload(@LoginUser Integer userId, @RequestParam("file") MultipartFile file) throws IOException {
        // P0-4：上传必须登录，杜绝匿名任意文件上传（存储滥用 / 恶意文件托管）
        if (userId == null) {
            return ResponseUtil.unlogin();
        }
        String originalFilename = file.getOriginalFilename();
        LeomallStorage leomallStorage = storageService.store(file.getInputStream(), file.getSize(), file.getContentType(), originalFilename);
        return ResponseUtil.ok(leomallStorage);
    }

    /**
     * 访问存储对象
     *
     * @param key 存储对象key
     * @return
     */
    @GetMapping("/fetch/{key:.+}")
    public ResponseEntity<Resource> fetch(@PathVariable String key) {
        if (!isSafeKey(key)) {
            return ResponseEntity.badRequest().build();
        }
        LeomallStorage leomallStorage = leomallStorageService.findByKey(key);
        if (leomallStorage == null) {
            return ResponseEntity.notFound().build();
        }
        Resource file = storageService.loadAsResource(key);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        // P0-4：Content-Type 由服务端按后缀判定，不信任上传时客户端声明的 type；
        // 非图片一律附件下载，禁止 HTML/SVG 等在商城同源内联执行（存储型 XSS）。
        MediaType mediaType = imageMediaType(key);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header("X-Content-Type-Options", "nosniff");
        if (mediaType == null) {
            return builder.contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                    .body(file);
        }
        return builder.contentType(mediaType).body(file);
    }

    /**
     * 访问存储对象
     *
     * @param key 存储对象key
     * @return
     */
    @GetMapping("/download/{key:.+}")
    public ResponseEntity<Resource> download(@PathVariable String key) {
        if (!isSafeKey(key)) {
            return ResponseEntity.badRequest().build();
        }
        LeomallStorage leomallStorage = leomallStorageService.findByKey(key);
        if (leomallStorage == null) {
            return ResponseEntity.notFound().build();
        }
        Resource file = storageService.loadAsResource(key);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    /** key 只允许「随机串+图片后缀」形态，拒绝任何路径分隔符与父目录引用 */
    private boolean isSafeKey(String key) {
        return key != null && !key.isEmpty()
                && !key.contains("..") && !key.contains("/") && !key.contains("\\");
    }

    /** 服务端判定的可内联图片类型；其余返回 null（走附件下载） */
    private MediaType imageMediaType(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return null;
    }

}
