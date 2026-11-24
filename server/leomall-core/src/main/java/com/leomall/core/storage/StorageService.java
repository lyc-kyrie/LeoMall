package com.leomall.core.storage;

import com.leomall.core.util.CharUtil;
import com.leomall.db.domain.LeomallStorage;
import com.leomall.db.service.LeomallStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * 提供存储服务类，所有存储服务均由该类对外提供
 */
public class StorageService {
    private String active;
    private Storage storage;
    @Autowired
    private LeomallStorageService leomallStorageService;

    /** P0-4：允许上传的图片后缀白名单（小写，含点） */
    private static final List<String> ALLOWED_SUFFIXES =
            Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    /**
     * 存储一个文件对象
     *
     * @param inputStream   文件输入流
     * @param contentLength 文件长度
     * @param contentType   文件类型
     * @param fileName      文件索引名
     */
    public LeomallStorage store(InputStream inputStream, long contentLength, String contentType, String fileName) {
        String key = generateKey(fileName);
        storage.store(inputStream, contentLength, contentType, key);

        String url = generateUrl(key);
        LeomallStorage storageInfo = new LeomallStorage();
        storageInfo.setName(fileName);
        storageInfo.setSize((int) contentLength);
        storageInfo.setType(contentType);
        storageInfo.setKey(key);
        storageInfo.setUrl(url);
        leomallStorageService.add(storageInfo);

        return storageInfo;
    }

    private String generateKey(String originalFilename) {
        // P0-4：后缀白名单 + 路径分隔符防御。
        // 原实现直接拼接原始后缀且无白名单，可上传 html/svg/js 等任意类型。
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        int index = originalFilename.lastIndexOf('.');
        if (index < 0 || index == originalFilename.length() - 1) {
            throw new IllegalArgumentException("文件必须包含合法后缀");
        }
        String suffix = originalFilename.substring(index).toLowerCase();
        if (suffix.contains("/") || suffix.contains("\\") || suffix.contains("..")) {
            throw new IllegalArgumentException("非法文件名");
        }
        if (!ALLOWED_SUFFIXES.contains(suffix)) {
            throw new IllegalArgumentException("仅支持图片文件（jpg/jpeg/png/gif/webp/bmp）");
        }

        String key = null;
        LeomallStorage storageInfo = null;

        do {
            key = CharUtil.getRandomString(20) + suffix;
            storageInfo = leomallStorageService.findByKey(key);
        }
        while (storageInfo != null);

        return key;
    }

    public Stream<Path> loadAll() {
        return storage.loadAll();
    }

    public Path load(String keyName) {
        return storage.load(keyName);
    }

    public Resource loadAsResource(String keyName) {
        return storage.loadAsResource(keyName);
    }

    public void delete(String keyName) {
        storage.delete(keyName);
    }

    private String generateUrl(String keyName) {
        return storage.generateUrl(keyName);
    }
}
