package com.leomall.db;

import org.junit.jupiter.api.Test;
import com.leomall.db.util.DbUtil;

import java.io.File;
import java.io.IOException;

public class DbUtilTest {
    @Test
    public void testBackup() throws IOException {
        File file = new File("test.sql");
        DbUtil.backup(file, "leomall", "leomall123456", "leomall");
    }

//    这个测试用例会重置leomall数据库，所以比较危险，请开发者注意
//    @Test
    public void testLoad() throws IOException {
        File file = new File("test.sql");
        DbUtil.load(file, "leomall", "leomall123456", "leomall");
    }
}
