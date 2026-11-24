package com.leomall.allinone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.web.WebAppConfiguration;

@WebAppConfiguration
@SpringBootTest
public class AllinoneConfigTest {
    @Autowired
    private Environment environment;

    @Test
    public void test() {
        // 测试获取application-core.yml配置信息
        System.out.println(environment.getProperty("leomall.express.appId"));
        // 测试获取application-db.yml配置信息
        System.out.println(environment.getProperty("spring.datasource.druid.url"));
        // 测试获取application-wx.yml配置信息
        System.out.println(environment.getProperty("leomall.wx.app-id"));
        // 测试获取application-admin.yml配置信息
//        System.out.println(environment.getProperty(""));
        // 测试获取application.yml配置信息
        System.out.println(environment.getProperty("logging.level.com.leomall.wx"));
        System.out.println(environment.getProperty("logging.level.com.leomall.admin"));
        System.out.println(environment.getProperty("logging.level.com.leomall"));
    }

}
