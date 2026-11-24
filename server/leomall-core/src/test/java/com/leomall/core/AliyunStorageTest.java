package com.leomall.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import com.leomall.core.storage.AliyunStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@WebAppConfiguration
@SpringBootTest
public class AliyunStorageTest {

    private final Log logger = LogFactory.getLog(AliyunStorageTest.class);
    @Autowired
    private AliyunStorage aliyunStorage;

    @Test
    public void test() throws IOException {
        String test = getClass().getClassLoader().getResource("leomall.png").getFile();
        File testFile = new File(test);
        aliyunStorage.store(new FileInputStream(test), testFile.length(), "image/png", "leomall.png");
        Resource resource = aliyunStorage.loadAsResource("leomall.png");
        String url = aliyunStorage.generateUrl("leomall.png");
        logger.info("test file " + test);
        logger.info("store file " + resource.getURI());
        logger.info("generate url " + url);
    }

}