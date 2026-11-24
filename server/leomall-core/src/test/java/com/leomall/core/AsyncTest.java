package com.leomall.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * 异步测试
 */
@WebAppConfiguration
@SpringBootTest
public class AsyncTest {

    @Autowired
    AsyncTask task;

    @Test
    public void test() {
        task.asyncMethod();
        task.nonasyncMethod();
    }
}
