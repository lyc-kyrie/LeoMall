package com.leomall.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import com.leomall.core.storage.QiniuStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@WebAppConfiguration
@SpringBootTest
public class QiniuStorageTest {

    private final Log logger = LogFactory.getLog(QiniuStorageTest.class);
    @Autowired
    private QiniuStorage qiniuStorage;

    @Test
    public void test() throws IOException {
        String test = getClass().getClassLoader().getResource("leomall.png").getFile();
        File testFile = new File(test);
        qiniuStorage.store(new FileInputStream(test), testFile.length(), "image/png", "leomall.png");
        Resource resource = qiniuStorage.loadAsResource("leomall.png");
        String url = qiniuStorage.generateUrl("leomall.png");
        logger.info("test file " + test);
        logger.info("store file " + resource.getURI());
        logger.info("generate url " + url);
    }

}