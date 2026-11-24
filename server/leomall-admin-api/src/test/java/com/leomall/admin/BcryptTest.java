package com.leomall.admin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.leomall.core.util.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.test.context.SpringBootTest;

/** 阶段2B：JUnit4 → JUnit5（遗留集成测试，surefire 默认排除，-Dtest 可显式运行） */
@SpringBootTest
public class BcryptTest {

    @Test
    public void test() {
        String rawPassword = "aaaaaa";
        String encodedPassword = "";
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        encodedPassword = bCryptPasswordEncoder.encode(rawPassword);

        System.out.println("rawPassword=" + rawPassword + " encodedPassword=" + encodedPassword);

        Assertions.assertTrue(bCryptPasswordEncoder.matches(rawPassword, encodedPassword));
    }
}